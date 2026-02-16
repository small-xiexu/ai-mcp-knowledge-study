package com.xbk.knowledge.config.vector;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 向量存储配置
 * 统一管理不同模型的向量表配置，便于多向量库切换
 *
 * @author xiexu
 */
@Data
@ConfigurationProperties(prefix = "vector.store")
public class VectorStoreProperties {

    /**
     * OpenAI 向量存储配置
     */
    private Store openai = new Store();

    /**
     * Ollama 向量存储配置
     */
    private Store ollama = new Store();

    /**
     * 向量存储配置项
     */
    @Data
    public static class Store {

        /**
         * 向量表名称
         */
        private String tableName;
    }
}
