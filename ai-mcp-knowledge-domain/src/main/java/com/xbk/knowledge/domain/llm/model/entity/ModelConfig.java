package com.xbk.knowledge.domain.llm.model.entity;

import com.xbk.knowledge.types.enums.ModelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 模型配置实体
 * 对应数据库表：ai_model_config
 *
 * 职责：领域实体，用于承载核心业务状态与生命周期
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfig {

    /**
     * 主键ID
     *
     * 为什么：用于持久化唯一标识
     */
    private Long id;

    /**
     * scopeId。
     *
     * 为什么：用于模型配置的组织归属标识；单组织模式下默认归属为 1。
     */

    /**
     * 模型名称
     *
     * 为什么：用于唯一性校验与展示
     */
    private String modelName;

    /**
     * 模型类型（OPENAI/ANTHROPIC/GEMINI）
     *
     * 为什么：决定调用协议与实现
     */
    private ModelType modelType;

    /**
     * API密钥
     *
     * 为什么：访问模型服务的鉴权凭证
     */
    private String apiKey;

    /**
     * API地址
     *
     * 为什么：模型服务的访问入口
     */
    private String baseUrl;

    /**
     * 是否启用（0:禁用 1:启用）
     *
     * 为什么：控制模型是否参与路由
     */
    private Boolean enabled;

    /**
     * 是否启用工具调用（0:禁用 1:启用）
     *
     * 为什么：控制工具能力开关
     */
    private Boolean toolEnabled;

    /**
     * 创建时间
     *
     * 为什么：用于审计与排序
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     *
     * 为什么：用于审计与变更追踪
     */
    private LocalDateTime updatedAt;

}
