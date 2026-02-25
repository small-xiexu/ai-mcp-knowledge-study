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
 * 对应数据库表ai_model_config
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
     * 用于持久化唯一标识
     */
    private Long id;

    /**
     * 模型名称
     *
     * 用于唯一性校验与展示
     */
    private String modelName;

    /**
     * 模型类型（OPENAI/ANTHROPIC/GEMINI）
     *
     * 决定调用协议与实现
     */
    private ModelType modelType;

    /**
     * API密钥
     *
     * 访问模型服务的鉴权凭证
     */
    private String apiKey;

    /**
     * API地址
     *
     * 模型服务的访问入口
     */
    private String baseUrl;

    /**
     * 对话补全路径
     *
     * 兼容不同 OpenAI 协议供应商的对话接口路径差异
     */
    private String completionsPath;

    /**
     * 向量嵌入路径
     *
     * 兼容不同 OpenAI 协议供应商的向量接口路径差异
     */
    private String embeddingsPath;

    /**
     * 是否启用（0:禁用 1:启用）
     *
     * 控制模型是否参与路由
     */
    private Boolean enabled;

    /**
     * 是否启用工具调用（0:禁用 1:启用）
     *
     * 控制工具能力开关
     */
    private Boolean toolEnabled;

    /**
     * Prompt 历史字符预算
     *
     * 控制该模型单次组装上下文时允许注入的历史文本规模
     */
    private Integer maxPromptChars;

    /**
     * Prompt 历史消息条数预算
     *
     * 控制该模型单次组装上下文时允许注入的历史消息条数
     */
    private Integer maxHistoryMessages;

    /**
     * 创建时间
     *
     * 用于审计与排序
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     *
     * 用于审计与变更追踪
     */
    private LocalDateTime updatedAt;

}
