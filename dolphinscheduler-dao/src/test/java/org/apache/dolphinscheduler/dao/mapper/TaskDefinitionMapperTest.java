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

package org.apache.dolphinscheduler.dao.mapper;

import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.dao.BaseDaoTest;
import org.apache.dolphinscheduler.dao.entity.ProcessTaskRelation;
import org.apache.dolphinscheduler.dao.entity.TaskDefinition;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.dao.model.WorkflowDefinitionCountDto;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class TaskDefinitionMapperTest extends BaseDaoTest {

    @Autowired
    private TaskDefinitionMapper taskDefinitionMapper;

    @Autowired
    private ProcessTaskRelationMapper processTaskRelationMapper;

    @Autowired
    private UserMapper userMapper;

    public TaskDefinition insertOne() {
        return insertOne(99);
    }

    public TaskDefinition insertOne(int userId) {
        TaskDefinition taskDefinition = new TaskDefinition();
        taskDefinition.setCode(888888L);
        taskDefinition.setName("unit-test");
        taskDefinition.setProjectCode(1L);
        taskDefinition.setTaskType("SHELL");
        taskDefinition.setUserId(userId);
        taskDefinition.setResourceIds("1");
        taskDefinition.setWorkerGroup("default");
        taskDefinition.setEnvironmentCode(1L);
        taskDefinition.setVersion(1);
        taskDefinition.setCreateTime(new Date());
        taskDefinition.setUpdateTime(new Date());
        taskDefinitionMapper.insert(taskDefinition);
        return taskDefinition;
    }

    /**
     * insert
     *
     * @return ProcessDefinition
     */
    private ProcessTaskRelation insertTaskRelation(long postTaskCode) {
        ProcessTaskRelation processTaskRelation = new ProcessTaskRelation();
        processTaskRelation.setName("def 1");
        processTaskRelation.setProjectCode(1L);
        processTaskRelation.setProcessDefinitionCode(1L);
        processTaskRelation.setPostTaskCode(postTaskCode);
        processTaskRelation.setPreTaskCode(0L);
        processTaskRelation.setUpdateTime(new Date());
        processTaskRelation.setCreateTime(new Date());
        processTaskRelationMapper.insert(processTaskRelation);
        return processTaskRelation;
    }

    @Test
    public void testInsert() {
        TaskDefinition taskDefinition = insertOne();
        Assertions.assertNotEquals(0, taskDefinition.getId().intValue());
    }

    @Test
    public void testQueryByDefinitionName() {
        TaskDefinition taskDefinition = insertOne();
        ProcessTaskRelation processTaskRelation = insertTaskRelation(taskDefinition.getCode());
        TaskDefinition result = taskDefinitionMapper.queryByName(taskDefinition.getProjectCode(),
                processTaskRelation.getProcessDefinitionCode(), taskDefinition.getName());

        Assertions.assertNotNull(result);
    }

    @Test
    public void testQueryByDefinitionCode() {
        TaskDefinition taskDefinition = insertOne();
        TaskDefinition result = taskDefinitionMapper.queryByCode(taskDefinition.getCode());
        Assertions.assertNotNull(result);

    }

    @Test
    public void testQueryAllDefinitionList() {
        TaskDefinition taskDefinition = insertOne();
        List<TaskDefinition> taskDefinitions =
                taskDefinitionMapper.queryAllDefinitionList(taskDefinition.getProjectCode());
        Assertions.assertNotEquals(0, taskDefinitions.size());

    }

    @Test
    public void testCountDefinitionGroupByUser() {
        User user = new User();
        user.setUserName("un");
        userMapper.insert(user);
        User un = userMapper.queryByUserNameAccurately("un");
        TaskDefinition taskDefinition = insertOne(un.getId());

        List<WorkflowDefinitionCountDto> users =
                taskDefinitionMapper.countDefinitionGroupByUser(new Long[]{taskDefinition.getProjectCode()});
        Assertions.assertNotEquals(0, users.size());

    }

    @Test
    public void testDeleteByCode() {
        TaskDefinition taskDefinition = insertOne();
        int i = taskDefinitionMapper.deleteByCode(taskDefinition.getCode());
        Assertions.assertNotEquals(0, i);

    }

    @Test
    public void testNullPropertyValueOfLocalParams() {
        String definitionJson =
                "{\"failRetryTimes\":\"0\",\"timeoutNotifyStrategy\":\"\",\"code\":\"5195043558720\",\"flag\":\"YES\",\"environmentCode\":\"-1\",\"taskDefinitionIndex\":2,\"taskPriority\":\"MEDIUM\",\"taskParams\":\"{\\\"preStatements\\\":null,\\\"postStatements\\\":null,\\\"type\\\":\\\"ADB_MYSQL\\\",\\\"database\\\":\\\"lijia\\\",\\\"sql\\\":\\\"create table nation_${random_serial_number} as select * from nation\\\",\\\"localParams\\\":[{\\\"direct\\\":2,\\\"type\\\":3,\\\"prop\\\":\\\"key\\\"}],\\\"Name\\\":\\\"create_table_as_select_nation\\\",\\\"FailRetryTimes\\\":0,\\\"dbClusterId\\\":\\\"amv-bp10o45925jpx959\\\",\\\"sendEmail\\\":false,\\\"displayRows\\\":10,\\\"limit\\\":10000,\\\"agentSource\\\":\\\"Workflow\\\",\\\"agentVersion\\\":\\\"Unkown\\\"}\",\"timeout\":\"0\",\"taskType\":\"ADB_MYSQL\",\"timeoutFlag\":\"CLOSE\",\"projectCode\":\"5191800302720\",\"name\":\"create_table_as_select_nation\",\"delayTime\":\"0\",\"workerGroup\":\"default\"}";
        TaskDefinition definition = JSONUtils.parseObject(definitionJson, TaskDefinition.class);

        Map<String, String> taskParamsMap = definition.getTaskParamMap();
        if (taskParamsMap != null) {
            Assertions.assertNull(taskParamsMap.get("key"));
        } else {
            Assertions.fail("Deserialize the task definition failed");
        }

        String newDefinitionJson = JSONUtils.toJsonString(definition);
        Assertions.assertNotNull("Serialize the task definition success", newDefinitionJson);
    }

    @Test
    public void testNullLocalParamsOfTaskParams() {
        String definitionJson =
                "{\"failRetryTimes\":\"0\",\"timeoutNotifyStrategy\":\"\",\"code\":\"5195043558720\",\"flag\":\"YES\",\"environmentCode\":\"-1\",\"taskDefinitionIndex\":2,\"taskPriority\":\"MEDIUM\",\"taskParams\":\"{\\\"preStatements\\\":null,\\\"postStatements\\\":null,\\\"type\\\":\\\"ADB_MYSQL\\\",\\\"database\\\":\\\"lijia\\\",\\\"sql\\\":\\\"create table nation_${random_serial_number} as select * from nation\\\",\\\"localParams\\\":null,\\\"Name\\\":\\\"create_table_as_select_nation\\\",\\\"FailRetryTimes\\\":0,\\\"dbClusterId\\\":\\\"amv-bp10o45925jpx959\\\",\\\"sendEmail\\\":false,\\\"displayRows\\\":10,\\\"limit\\\":10000,\\\"agentSource\\\":\\\"Workflow\\\",\\\"agentVersion\\\":\\\"Unkown\\\"}\",\"timeout\":\"0\",\"taskType\":\"ADB_MYSQL\",\"timeoutFlag\":\"CLOSE\",\"projectCode\":\"5191800302720\",\"name\":\"create_table_as_select_nation\",\"delayTime\":\"0\",\"workerGroup\":\"default\"}";
        TaskDefinition definition = JSONUtils.parseObject(definitionJson, TaskDefinition.class);

        Assertions.assertNull(definition.getTaskParamMap(), "Serialize the task definition success");
    }
}
