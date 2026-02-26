package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.llm.model.aggregate.ModelConfigAggregate;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.domain.llm.model.valobj.ModelNameQuery;
import com.xbk.knowledge.infrastructure.dao.IModelConfigDao;
import com.xbk.knowledge.infrastructure.dao.po.ModelConfigPO;
import com.xbk.knowledge.infrastructure.repository.model.ModelConfigRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

/**
 * 验证模型配置仓储的空参处理与新增逻辑。
 *
 * @author xiexu
 */
public class ModelConfigRepositoryImplTest {

    /**
     * 对外暴露 shouldReturnEmptyWhenQueryNameMissing 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldReturnEmptyWhenQueryNameMissing() {
        IModelConfigDao modelConfigMapper = Mockito.mock(IModelConfigDao.class);
        ModelConfigRepositoryImpl repository = new ModelConfigRepositoryImpl(modelConfigMapper);

        Optional<ModelConfig> result = repository.findByModelName(new ModelNameQuery(null));

        assertTrue(!result.isPresent());
    }

    /**
     * 对外暴露 shouldSaveNewModel 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldSaveNewModel() {
        IModelConfigDao modelConfigMapper = Mockito.mock(IModelConfigDao.class);
        ModelConfigRepositoryImpl repository = new ModelConfigRepositoryImpl(modelConfigMapper);

        ModelConfig modelConfig = ModelConfig.builder().modelName("m1").build();
        ModelConfigAggregate aggregate = ModelConfigAggregate.builder()
                .modelConfig(modelConfig)
                .build();

        doAnswer(invocation -> {
            ModelConfigPO arg = invocation.getArgument(0);
            arg.setId(1L);
            return null;
        }).when(modelConfigMapper).insertModelConfig(any(ModelConfigPO.class));

        repository.save(aggregate);

        assertTrue(modelConfig.getCreatedAt() != null);
        assertTrue(modelConfig.getUpdatedAt() != null);
        verify(modelConfigMapper).insertModelConfig(any(ModelConfigPO.class));
    }

    /**
     * 对外暴露 shouldReturnEmptyListWhenEnabledQueryMissing 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldReturnEmptyListWhenEnabledQueryMissing() {
        IModelConfigDao modelConfigMapper = Mockito.mock(IModelConfigDao.class);
        ModelConfigRepositoryImpl repository = new ModelConfigRepositoryImpl(modelConfigMapper);

        assertTrue(repository.findByEnabled((EnabledQuery) null).isEmpty());
    }
}
