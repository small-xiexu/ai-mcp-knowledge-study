package com.xbk.knowledge.api.dto.agent;

import com.xbk.knowledge.types.common.BaseRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * PromptTemplate 创建请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PromptTemplateCreateRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * GLOBAL/ORG，默认 ORG。
     */
    @Builder.Default
    private String scope = "ORG";

    @NotBlank(message = "templateCode 不能为空")
    private String templateCode;

    @NotBlank(message = "templateName 不能为空")
    private String templateName;

    @NotBlank(message = "content 不能为空")
    private String content;

    /**
     * 变量契约 JSON（对象/数组均可，由前端约定）。
     */
    private String variableSpecJson;
}

