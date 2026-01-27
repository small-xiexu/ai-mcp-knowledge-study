package com.xbk.knowledge.orchestration.controller.dto;

import com.xbk.knowledge.orchestration.model.enums.ModelType;
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
     * 模型类型（OPENAI/ANTHROPIC/GEMINI）
     */
    @NotNull(message = "模型类型不能为空")
    private ModelType modelType;

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
