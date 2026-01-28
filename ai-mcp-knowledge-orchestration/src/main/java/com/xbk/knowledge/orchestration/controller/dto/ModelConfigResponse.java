package com.xbk.knowledge.orchestration.controller.dto;

import com.xbk.knowledge.orchestration.domain.entity.ModelCapability;
import com.xbk.knowledge.orchestration.model.enums.ProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模型配置响应 DTO
 * 用于返回模型配置信息
 *
 * @author xiexu
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfigResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * API 提供商类型
     * 标识使用哪个提供商的 API 协议（OPENAI/ANTHROPIC/GEMINI）
     */
    private ProviderType providerType;

    /**
     * API地址
     */
    private String baseUrl;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 模型能力
     */
    private ModelCapability capability;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
