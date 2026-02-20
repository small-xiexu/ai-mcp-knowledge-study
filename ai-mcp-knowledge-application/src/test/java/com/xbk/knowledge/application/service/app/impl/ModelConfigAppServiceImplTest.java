package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.adapter.repository.model.ModelActivationRepository;
import com.xbk.knowledge.domain.service.model.IModelConfigService;
import com.xbk.knowledge.application.provider.ModelProviderFactory;
import com.xbk.knowledge.application.service.armory.factory.DefaultAiClientArmoryStrategyFactory;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;

/**
 * 验证模型配置应用服务的委托调用，避免编排层漏转发。
 *
 * @author xiexu
 */
public class ModelConfigAppServiceImplTest {

    /**
     * 对外暴露 shouldDelegateCreateAndDelete 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldDelegateCreateAndDelete() {
        IModelConfigService domainService = Mockito.mock(IModelConfigService.class);
        ModelActivationRepository activationRepository = Mockito.mock(ModelActivationRepository.class);
        ModelProviderFactory providerFactory = Mockito.mock(ModelProviderFactory.class);
        DefaultAiClientArmoryStrategyFactory armoryStrategyFactory = Mockito.mock(DefaultAiClientArmoryStrategyFactory.class);
        ModelConfigAppServiceImpl appService =
                new ModelConfigAppServiceImpl(domainService, activationRepository, providerFactory, armoryStrategyFactory);

        ModelConfig modelConfig = ModelConfig.builder().modelName("m1").build();
        appService.createModelConfig(modelConfig);
        appService.deleteModelConfig(new IdQuery(1L));

        verify(domainService).createModelConfig(modelConfig);
        verify(domainService).deleteModelConfig(Mockito.any(IdQuery.class));
    }
}
