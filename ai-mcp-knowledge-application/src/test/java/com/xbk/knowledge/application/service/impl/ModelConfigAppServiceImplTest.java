package com.xbk.knowledge.application.service.impl;

import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.service.IModelConfigService;
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
        ModelConfigAppServiceImpl appService = new ModelConfigAppServiceImpl(domainService);

        ModelConfig modelConfig = ModelConfig.builder().modelName("m1").build();
        appService.createModelConfig(modelConfig);
        appService.deleteModelConfig(new IdQuery(1L));

        verify(domainService).createModelConfig(modelConfig);
        verify(domainService).deleteModelConfig(Mockito.any(IdQuery.class));
    }
}
