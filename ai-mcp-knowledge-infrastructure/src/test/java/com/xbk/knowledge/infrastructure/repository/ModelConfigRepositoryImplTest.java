package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.aggregate.model.ModelConfigAggregate;
import com.xbk.knowledge.domain.model.entity.ModelCapability;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.vo.common.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.model.ModelNameQuery;
import com.xbk.knowledge.infrastructure.mapper.ModelCapabilityMapper;
import com.xbk.knowledge.infrastructure.mapper.ModelConfigMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * 验证模型配置仓储的空参处理与能力持久化逻辑，避免能力数据丢失。
 *
 * @author xiexu
 */
public class ModelConfigRepositoryImplTest {

    /**
     * 对外暴露 shouldReturnEmptyWhenQueryNameMissing 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldReturnEmptyWhenQueryNameMissing() {
        ModelConfigMapper modelConfigMapper = Mockito.mock(ModelConfigMapper.class);
        ModelCapabilityMapper capabilityMapper = Mockito.mock(ModelCapabilityMapper.class);
        ModelConfigRepositoryImpl repository = new ModelConfigRepositoryImpl(modelConfigMapper, capabilityMapper);

        Optional<ModelConfig> result = repository.findByModelName(new ModelNameQuery(null));

        assertTrue(!result.isPresent());
    }

    /**
     * 对外暴露 shouldSaveNewModelWithCapability 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldSaveNewModelWithCapability() {
        ModelConfigMapper modelConfigMapper = Mockito.mock(ModelConfigMapper.class);
        ModelCapabilityMapper capabilityMapper = Mockito.mock(ModelCapabilityMapper.class);
        ModelConfigRepositoryImpl repository = new ModelConfigRepositoryImpl(modelConfigMapper, capabilityMapper);

        ModelCapability capability = ModelCapability.builder().qualityScore(80).build();
        ModelConfig modelConfig = ModelConfig.builder().modelName("m1").capability(capability).build();
        ModelConfigAggregate aggregate = ModelConfigAggregate.builder()
                .modelConfig(modelConfig)
                .modelCapability(capability)
                .build();

        doAnswer(invocation -> {
            ModelConfig arg = invocation.getArgument(0);
            arg.setId(1L);
            return null;
        }).when(modelConfigMapper).insertModelConfig(any(ModelConfig.class));

        repository.save(aggregate);

        ArgumentCaptor<ModelCapability> captor = ArgumentCaptor.forClass(ModelCapability.class);
        Mockito.verify(capabilityMapper).insertModelCapability(captor.capture());
        assertEquals(1L, captor.getValue().getModelId());
    }

    /**
     * 对外暴露 shouldReturnEmptyListWhenEnabledQueryMissing 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldReturnEmptyListWhenEnabledQueryMissing() {
        ModelConfigMapper modelConfigMapper = Mockito.mock(ModelConfigMapper.class);
        ModelCapabilityMapper capabilityMapper = Mockito.mock(ModelCapabilityMapper.class);
        ModelConfigRepositoryImpl repository = new ModelConfigRepositoryImpl(modelConfigMapper, capabilityMapper);

        assertTrue(repository.findByEnabled((EnabledQuery) null).isEmpty());
    }
}
