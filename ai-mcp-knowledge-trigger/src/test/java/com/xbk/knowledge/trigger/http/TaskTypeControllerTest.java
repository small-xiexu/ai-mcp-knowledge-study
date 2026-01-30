package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.dto.task.TaskTypeQueryRequest;
import com.xbk.knowledge.api.dto.task.TaskTypeResponse;
import com.xbk.knowledge.application.service.ModelConfigAppService;
import com.xbk.knowledge.application.service.TaskTypeAppService;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.entity.TaskType;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypePageQuery;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.types.enums.ModelType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证任务类型 Controller 的分页转换与首选模型名称填充。
 *
 * @author xiexu
 */
public class TaskTypeControllerTest {

    /**
     * 对外暴露 shouldListTaskTypesWithPreferredModelName 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldListTaskTypesWithPreferredModelName() {
        TaskTypeAppService taskTypeAppService = Mockito.mock(TaskTypeAppService.class);
        ModelConfigAppService modelConfigAppService = Mockito.mock(ModelConfigAppService.class);
        TaskTypeController controller = new TaskTypeController(taskTypeAppService, modelConfigAppService);

        TaskType taskType = TaskType.builder()
                .id(1L)
                .taskCode("code")
                .taskName("name")
                .preferredModelId(10L)
                .build();
        PageResult<TaskType> pageResult = PageResult.of(Collections.<TaskType>singletonList(taskType), 1L, 1, 10);
        when(taskTypeAppService.queryTaskTypePage(any(TaskTypePageQuery.class))).thenReturn(pageResult);

        ModelConfig modelConfig = ModelConfig.builder().id(10L).modelName("m1").modelType(ModelType.OPENAI).build();
        when(modelConfigAppService.queryModelConfigById(any(IdQuery.class))).thenReturn(modelConfig);

        TaskTypeQueryRequest request = new TaskTypeQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        request.validate();

        Result<PageResult<TaskTypeResponse>> result = controller.listTaskTypes(request);

        assertEquals("m1", result.getData().getRecords().get(0).getPreferredModelName());

        ArgumentCaptor<TaskTypePageQuery> captor = ArgumentCaptor.forClass(TaskTypePageQuery.class);
        verify(taskTypeAppService).queryTaskTypePage(captor.capture());
        assertEquals(0, captor.getValue().getOffset());
        assertEquals(10, captor.getValue().getPageSize());
    }
}
