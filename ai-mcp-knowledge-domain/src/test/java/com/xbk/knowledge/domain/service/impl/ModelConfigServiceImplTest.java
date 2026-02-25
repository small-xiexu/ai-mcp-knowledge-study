package com.xbk.knowledge.domain.service.impl;

import com.xbk.knowledge.domain.llm.model.aggregate.ModelConfigAggregate;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.llm.model.valobj.ModelConfigPageQuery;
import com.xbk.knowledge.domain.llm.model.valobj.ModelNameQuery;
import com.xbk.knowledge.domain.llm.adapter.repository.ModelConfigRepository;
import com.xbk.knowledge.types.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证模型配置领域服务的校验与聚合更新逻辑，避免配置数据异常。
 *
 * @author xiexu
 */
public class ModelConfigServiceImplTest {

    /**
     * 模型配置仓储。
     */
    private ModelConfigRepository modelConfigRepository;

    /**
     * 模型配置领域服务实现。
     */
    private ModelConfigServiceImpl service;

    /**
     * 对外暴露 setUp 作为调用入口，便于上层复用。
     */
    @BeforeEach
    public void setUp() {
        modelConfigRepository = Mockito.mock(ModelConfigRepository.class);
        service = new ModelConfigServiceImpl(modelConfigRepository);
    }

    /**
     * 对外暴露 shouldRejectNullPageQuery 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldRejectNullPageQuery() {
        assertThrows(IllegalArgumentException.class, () -> service.queryModelConfigPage(null));
    }

    /**
     * 对外暴露 shouldDefaultPageQueryWhenValuesMissing 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldDefaultPageQueryWhenValuesMissing() {
        ModelConfigPageQuery query = new ModelConfigPageQuery(null, null);
        List<ModelConfig> models = Collections.emptyList();
        when(modelConfigRepository.findPage(any(ModelConfigPageQuery.class))).thenReturn(models);
        when(modelConfigRepository.countAll()).thenReturn(0L);

        service.queryModelConfigPage(query);

        ArgumentCaptor<ModelConfigPageQuery> captor = ArgumentCaptor.forClass(ModelConfigPageQuery.class);
        verify(modelConfigRepository).findPage(captor.capture());
        assertEquals(0, captor.getValue().getOffset());
        assertEquals(10, captor.getValue().getPageSize());
    }

    /**
     * 对外暴露 shouldRejectDuplicateModelNameOnCreate 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldRejectDuplicateModelNameOnCreate() {
        ModelConfig modelConfig = ModelConfig.builder().modelName("gpt").build();
        when(modelConfigRepository.findByModelName(any(ModelNameQuery.class)))
                .thenReturn(Optional.of(ModelConfig.builder().build()));

        assertThrows(IllegalArgumentException.class, () -> service.createModelConfig(modelConfig));
    }

    /**
     * 对外暴露 shouldSetTimestampsOnCreate 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldSetTimestampsOnCreate() {
        ModelConfig modelConfig = ModelConfig.builder()
                .modelName("gpt")
                .build();
        when(modelConfigRepository.findByModelName(any(ModelNameQuery.class)))
                .thenReturn(Optional.empty());
        when(modelConfigRepository.save(any(ModelConfigAggregate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ModelConfig saved = service.createModelConfig(modelConfig);

        assertTrue(saved.getCreatedAt() != null);
        assertTrue(saved.getUpdatedAt() != null);
    }

    /**
     * 对外暴露 shouldRejectUpdateWithoutId 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldRejectUpdateWithoutId() {
        ModelConfig modelConfig = ModelConfig.builder().build();
        assertThrows(IllegalArgumentException.class, () -> service.updateModelConfig(modelConfig));
    }

    /**
     * 对外暴露 shouldUpdateFields 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldUpdateFields() {
        ModelConfig existing = ModelConfig.builder()
                .id(1L)
                .modelName("old")
                .build();
        when(modelConfigRepository.findById(any(IdQuery.class))).thenReturn(Optional.of(existing));
        when(modelConfigRepository.findByModelName(any(ModelNameQuery.class))).thenReturn(Optional.of(existing));
        when(modelConfigRepository.save(any(ModelConfigAggregate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ModelConfig request = ModelConfig.builder()
                .id(1L)
                .modelName("new")
                .apiKey("k")
                .baseUrl("url")
                .enabled(true)
                .toolEnabled(true)
                .build();

        ModelConfig updated = service.updateModelConfig(request);

        assertEquals("new", updated.getModelName());
        assertEquals("k", updated.getApiKey());
        assertEquals("url", updated.getBaseUrl());
        assertTrue(updated.getEnabled());
        assertTrue(updated.getToolEnabled());
    }

    /**
     * 对外暴露 shouldThrowWhenDeleteMissingModel 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldThrowWhenDeleteMissingModel() {
        when(modelConfigRepository.existsById(any(IdQuery.class))).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.deleteModelConfig(new IdQuery(1L)));
    }

}
