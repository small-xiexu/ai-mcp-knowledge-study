package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.domain.model.entity.TaskType;
import com.xbk.knowledge.domain.service.task.ITaskTypeService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;

/**
 * 验证任务类型应用服务的委托行为，确保用例链路完整。
 *
 * @author xiexu
 */
public class TaskTypeAppServiceImplTest {

    /**
     * 对外暴露 shouldDelegateCreate 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldDelegateCreate() {
        ITaskTypeService domainService = Mockito.mock(ITaskTypeService.class);
        TaskTypeAppServiceImpl appService = new TaskTypeAppServiceImpl(domainService);

        TaskType taskType = TaskType.builder().taskCode("code").build();
        appService.createTaskType(taskType);

        verify(domainService).createTaskType(taskType);
    }
}
