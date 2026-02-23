package com.xbk.knowledge.api.dto.agentenhancer;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * AgentEnhancer 保存请求（新增/更新）。
 *
 * @author sxie
 */
@Data
public class AgentEnhancerSaveRequest {

    private Long id;

    @NotBlank(message = "agentEnhancerCode 不能为空")
    private String agentEnhancerCode;

    @NotBlank(message = "agentEnhancerName 不能为空")
    private String agentEnhancerName;

    @NotBlank(message = "agentEnhancerType 不能为空")
    private String agentEnhancerType;

    private Boolean enabled;

    /**
     * JSON 字符串（可空）。
     */
    private String configJson;
}

