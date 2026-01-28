package com.xbk.knowledge.orchestration.controller.dto;

import com.xbk.knowledge.orchestration.model.enums.ProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 模型配置请求 DTO
 * 用于创建和更新模型配置
 *
 * @author xiexu
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfigRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模型名称
     */
    @NotBlank(message = "模型名称不能为空")
    private String modelName;

    /**
     * API 提供商类型
     * 标识使用哪个提供商的 API 协议（OPENAI/ANTHROPIC/GEMINI）
     *
     * <p>注意：这里表示的是 API 协议类型，而非具体的模型名称
     * <ul>
     *   <li>OPENAI：使用 OpenAI 兼容协议，可对接 GPT-4、DeepSeek、智谱等</li>
     *   <li>ANTHROPIC：使用 Anthropic 协议，对接 Claude 系列</li>
     *   <li>GEMINI：使用 Google Gemini 协议</li>
     * </ul>
     */
    @NotNull(message = "API 提供商类型不能为空")
    private ProviderType providerType;

    /**
     * API密钥
     */
    @NotBlank(message = "API密钥不能为空")
    private String apiKey;

    /**
     * API地址
     */
    @NotBlank(message = "API地址不能为空")
    private String baseUrl;

    /**
     * 是否启用
     */
    @Builder.Default
    private Boolean enabled = true;

    /**
     * 优先级（数字越大优先级越高）
     */
    @Builder.Default
    private Integer priority = 0;

    /**
     * 模型能力配置
     */
    private ModelCapabilityRequest capability;
}
