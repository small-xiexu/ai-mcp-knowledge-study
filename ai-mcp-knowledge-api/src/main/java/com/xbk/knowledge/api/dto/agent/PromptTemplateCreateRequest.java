package com.xbk.knowledge.api.dto.agent;

import com.xbk.knowledge.types.common.BaseRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * PromptTemplate 创建参数。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PromptTemplateCreateRequest extends BaseRequest {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 模板编码
     */
    @NotBlank(message = "templateCode 不能为空")
    private String templateCode;

    /**
     * 模板名称
     */
    @NotBlank(message = "templateName 不能为空")
    private String templateName;

    /**
     * 输入内容
     */
    @NotBlank(message = "content 不能为空")
    private String content;

    /**
     * 变量契约 JSON（对象/数组均可，由前端约定）。
     */
    private String variableSpecJson;
}
