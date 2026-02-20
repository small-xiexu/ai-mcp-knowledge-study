package com.xbk.knowledge.application.service.selection.config;

import com.xbk.knowledge.application.service.selection.chain.ModelSelectionChain;
import com.xbk.knowledge.application.service.selection.factory.ModelSelectionChainFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 模型选择责任链配置
 * 职责：对外暴露责任链实例，装配交由工厂统一负责
 *
 * @author xiexu
 */
@Configuration
public class ModelSelectionChainConfig {

    /**
     * 责任链装配顺序：显式策略 > 默认兜底
     *
     * @param factory 责任链工厂
     * @return 模型选择责任链
     */
    @Bean
    public ModelSelectionChain modelSelectionChain(ModelSelectionChainFactory factory) {
        return factory.openChain();
    }
}
