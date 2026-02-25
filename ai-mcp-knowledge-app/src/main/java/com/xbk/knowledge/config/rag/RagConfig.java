package com.xbk.knowledge.config.rag;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 相关配置
 *
 * 职责：提供知识库处理所需的基础组件
 * @author sxie
 */
@Configuration
public class RagConfig {

    /**
     * TokenTextSplitter
     * 默认分割器用于文档切块
     * 
     * @return TokenTextSplitter
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }
}
