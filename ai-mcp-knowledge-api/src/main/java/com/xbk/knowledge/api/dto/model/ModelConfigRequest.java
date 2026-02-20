package com.xbk.knowledge.api.dto.model;

import com.xbk.knowledge.types.common.BaseRequest;
import com.xbk.knowledge.types.enums.ModelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 模型配置请求 DTO
 * 用于创建和更新模型配置
 *
 * 职责：接口层 DTO，用于承载请求/响应参数并保证传输边界稳定
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ModelConfigRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 模型 ID（更新时必填，创建时不填）
     */
    private Long id;

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
     * 是否启用工具调用
     */
    @Builder.Default
    private Boolean toolEnabled = true;

}
