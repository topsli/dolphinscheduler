/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.server.master.runner.task.subworkflow;

import org.apache.dolphinscheduler.common.enums.WorkflowExecutionStatus;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.dao.entity.ProcessInstance;
import org.apache.dolphinscheduler.dao.entity.TaskInstance;
import org.apache.dolphinscheduler.dao.repository.ProcessInstanceDao;
import org.apache.dolphinscheduler.extract.base.client.SingletonJdkDynamicRpcClientProxyFactory;
import org.apache.dolphinscheduler.extract.master.ITaskInstanceExecutionEventListener;
import org.apache.dolphinscheduler.extract.master.transportor.WorkflowInstanceStateChangeEvent;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.parameters.SubProcessParameters;
import org.apache.dolphinscheduler.server.master.exception.MasterTaskExecuteException;
import org.apache.dolphinscheduler.server.master.runner.WorkflowExecuteRunnable;
import org.apache.dolphinscheduler.server.master.runner.execute.AsyncTaskExecuteFunction;
import org.apache.dolphinscheduler.server.master.runner.task.BaseAsyncLogicTask;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.core.type.TypeReference;

@Slf4j
public class SubWorkflowLogicTask extends BaseAsyncLogicTask<SubProcessParameters> {

    public static final String TASK_TYPE = "SUB_PROCESS";
    private final WorkflowExecuteRunnable workflowExecuteRunnable;
    private final ProcessInstanceDao processInstanceDao;

    public SubWorkflowLogicTask(TaskExecutionContext taskExecutionContext,
                                WorkflowExecuteRunnable workflowExecuteRunnable,
                                ProcessInstanceDao processInstanceDao) {
        super(taskExecutionContext,
                JSONUtils.parseObject(taskExecutionContext.getTaskParams(), new TypeReference<SubProcessParameters>() {
                }));
        this.workflowExecuteRunnable = workflowExecuteRunnable;
        this.processInstanceDao = processInstanceDao;
    }

    @Override
    public AsyncTaskExecuteFunction getAsyncTaskExecuteFunction() {
        // todo: create sub workflow instance here?
        return new SubWorkflowAsyncTaskExecuteFunction(taskExecutionContext, processInstanceDao);
    }

    @Override
    public void pause() throws MasterTaskExecuteException {
        if (workflowExecuteRunnable == null) {
            log.warn("Cannot find WorkflowExecuteRunnable");
            return;
        }
        ProcessInstance subProcessInstance =
                processInstanceDao.querySubProcessInstanceByParentId(taskExecutionContext.getProcessInstanceId(),
                        taskExecutionContext.getTaskInstanceId());
        if (subProcessInstance == null) {
            log.info("SubWorkflow instance is null");
            return;
        }
        TaskInstance taskInstance =
                workflowExecuteRunnable.getTaskInstance(taskExecutionContext.getTaskInstanceId()).orElse(null);
        if (taskInstance == null) {
            // we don't need to do this check, the task instance shouldn't be null
            log.info("TaskInstance is null");
            return;
        }
        if (taskInstance.getState().isFinished()) {
            log.info("The task instance is finished, no need to pause");
            return;
        }
        subProcessInstance.setStateWithDesc(WorkflowExecutionStatus.READY_PAUSE, "ready pause sub workflow");
        processInstanceDao.updateById(subProcessInstance);
        try {
            sendToSubProcess(taskExecutionContext, subProcessInstance);
            log.info("Success send pause request to SubWorkflow's master: {}", subProcessInstance.getHost());
        } catch (Exception e) {
            throw new MasterTaskExecuteException(String.format("Send pause request to SubWorkflow's master: %s failed",
                    subProcessInstance.getHost()), e);
        }
    }

    @Override
    public void kill() {
        if (workflowExecuteRunnable == null) {
            log.warn("Cannot find WorkflowExecuteRunnable");
            return;
        }
        ProcessInstance subProcessInstance =
                processInstanceDao.querySubProcessInstanceByParentId(taskExecutionContext.getProcessInstanceId(),
                        taskExecutionContext.getTaskInstanceId());
        if (subProcessInstance == null) {
            log.info("SubWorkflow instance is null");
            return;
        }
        TaskInstance taskInstance =
                workflowExecuteRunnable.getTaskInstance(taskExecutionContext.getTaskInstanceId()).orElse(null);
        if (taskInstance == null) {
            // we don't need to do this check, the task instance shouldn't be null
            log.info("TaskInstance is null");
            return;
        }
        if (subProcessInstance.getState().isFinished()) {
            log.info("The subProcessInstance is finished, no need to pause");
            return;
        }
        subProcessInstance.setStateWithDesc(WorkflowExecutionStatus.READY_STOP, "ready stop by kill task");
        processInstanceDao.updateById(subProcessInstance);
        try {
            sendToSubProcess(taskExecutionContext, subProcessInstance);
            log.info("Success send kill request to SubWorkflow's master: {}", subProcessInstance.getHost());
        } catch (Exception e) {
            log.error("Send kill request to SubWorkflow's master: {} failed", subProcessInstance.getHost(), e);
        }
    }

    private void sendToSubProcess(TaskExecutionContext taskExecutionContext,
                                  ProcessInstance subProcessInstance) {
        final ITaskInstanceExecutionEventListener iTaskInstanceExecutionEventListener =
                SingletonJdkDynamicRpcClientProxyFactory
                        .getProxyClient(subProcessInstance.getHost(), ITaskInstanceExecutionEventListener.class);
        final WorkflowInstanceStateChangeEvent workflowInstanceStateChangeEvent = new WorkflowInstanceStateChangeEvent(
                subProcessInstance.getId(),
                taskExecutionContext.getTaskInstanceId(),
                subProcessInstance.getState(),
                subProcessInstance.getId(),
                0);
        iTaskInstanceExecutionEventListener.onWorkflowInstanceInstanceStateChange(workflowInstanceStateChangeEvent);
    }
}
