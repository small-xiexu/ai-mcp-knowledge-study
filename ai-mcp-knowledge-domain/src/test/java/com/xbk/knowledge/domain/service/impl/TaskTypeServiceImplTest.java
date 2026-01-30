package com.xbk.knowledge.domain.service.impl;

import com.xbk.knowledge.domain.model.aggregate.task.TaskTypeAggregate;
import com.xbk.knowledge.domain.model.entity.TaskType;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypeCodeQuery;
import com.xbk.knowledge.domain.repository.ModelConfigRepository;
import com.xbk.knowledge.domain.repository.TaskTypeRepository;
import com.xbk.knowledge.types.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证任务类型领域服务的唯一性与首选模型校验，避免配置悬挂。
 *
 * @author xiexu
 */
public class TaskTypeServiceImplTest {

    private TaskTypeRepository taskTypeRepository;
    private ModelConfigRepository modelConfigRepository;
    private TaskTypeServiceImpl service;

    /**
     * 对外暴露 setUp 作为调用入口，便于上层复用。
     */
    @BeforeEach
    public void setUp() {
        taskTypeRepository = Mockito.mock(TaskTypeRepository.class);
        modelConfigRepository = Mockito.mock(ModelConfigRepository.class);
        service = new TaskTypeServiceImpl(taskTypeRepository, modelConfigRepository);
    }

    /**
     * 对外暴露 shouldRejectDuplicateTaskCodeOnCreate 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldRejectDuplicateTaskCodeOnCreate() {
        TaskType taskType = TaskType.builder().taskCode("code").build();
        when(taskTypeRepository.findByTaskCode(any(TaskTypeCodeQuery.class)))
                .thenReturn(Optional.of(TaskType.builder().build()));

        assertThrows(IllegalArgumentException.class, () -> service.createTaskType(taskType));
    }

    /**
     * 对外暴露 shouldRejectMissingPreferredModel 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldRejectMissingPreferredModel() {
        TaskType taskType = TaskType.builder()
                .taskCode("code")
                .preferredModelId(10L)
                .build();
        when(taskTypeRepository.findByTaskCode(any(TaskTypeCodeQuery.class))).thenReturn(Optional.empty());
        when(modelConfigRepository.existsById(any(IdQuery.class))).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.createTaskType(taskType));
    }

    /**
     * 对外暴露 shouldSetTimestampsOnCreate 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldSetTimestampsOnCreate() {
        TaskType taskType = TaskType.builder()
                .taskCode("code")
                .taskName("name")
                .preferredModelId(1L)
                .build();
        when(taskTypeRepository.findByTaskCode(any(TaskTypeCodeQuery.class))).thenReturn(Optional.empty());
        when(modelConfigRepository.existsById(any(IdQuery.class))).thenReturn(true);
        when(taskTypeRepository.save(any(TaskTypeAggregate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskType saved = service.createTaskType(taskType);

        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    /**
     * 对外暴露 shouldRejectUpdateWithoutId 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldRejectUpdateWithoutId() {
        TaskType taskType = TaskType.builder().build();
        assertThrows(IllegalArgumentException.class, () -> service.updateTaskType(taskType));
    }

    /**
     * 对外暴露 shouldRejectDuplicateTaskCodeOnUpdate 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldRejectDuplicateTaskCodeOnUpdate() {
        TaskType existing = TaskType.builder().id(1L).taskCode("code").build();
        when(taskTypeRepository.findById(any(IdQuery.class))).thenReturn(Optional.of(existing));
        when(taskTypeRepository.findByTaskCode(any(TaskTypeCodeQuery.class)))
                .thenReturn(Optional.of(TaskType.builder().id(2L).build()));

        TaskType request = TaskType.builder().id(1L).taskCode("code").build();

        assertThrows(IllegalArgumentException.class, () -> service.updateTaskType(request));
    }

    /**
     * 对外暴露 shouldDeleteWhenExists 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldDeleteWhenExists() {
        when(taskTypeRepository.existsById(any(IdQuery.class))).thenReturn(true);

        service.deleteTaskType(new IdQuery(1L));

        ArgumentCaptor<IdQuery> captor = ArgumentCaptor.forClass(IdQuery.class);
        verify(taskTypeRepository).deleteById(captor.capture());
        assertEquals(1L, captor.getValue().getId());
    }
}
