package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.aggregate.task.TaskTypeAggregate;
import com.xbk.knowledge.domain.model.entity.TaskType;
import com.xbk.knowledge.domain.model.vo.task.TaskTypeCodeQuery;
import com.xbk.knowledge.infrastructure.mapper.task.TaskTypeMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 验证任务类型仓储的空参处理与时间戳补齐逻辑。
 *
 * @author xiexu
 */
public class TaskTypeRepositoryImplTest {

    /**
     * 对外暴露 shouldReturnEmptyWhenCodeMissing 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldReturnEmptyWhenCodeMissing() {
        TaskTypeMapper mapper = Mockito.mock(TaskTypeMapper.class);
        TaskTypeRepositoryImpl repository = new TaskTypeRepositoryImpl(mapper);

        Optional<TaskType> result = repository.findByTaskCode(new TaskTypeCodeQuery(null));

        assertTrue(!result.isPresent());
    }

    /**
     * 对外暴露 shouldSetTimestampsOnInsert 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldSetTimestampsOnInsert() {
        TaskTypeMapper mapper = Mockito.mock(TaskTypeMapper.class);
        TaskTypeRepositoryImpl repository = new TaskTypeRepositoryImpl(mapper);
        when(mapper.insertTaskType(any(TaskType.class))).thenReturn(1);

        TaskType taskType = TaskType.builder().taskCode("code").build();
        TaskTypeAggregate aggregate = TaskTypeAggregate.builder().taskType(taskType).build();

        TaskTypeAggregate saved = repository.save(aggregate);

        assertNotNull(saved.getTaskType().getCreatedAt());
        assertNotNull(saved.getTaskType().getUpdatedAt());
    }
}
