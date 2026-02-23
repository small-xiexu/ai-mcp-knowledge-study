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

    /**
     * 主键ID
     */
    private Long id;

    /**
     * AgentEnhancer编码
     */
    @NotBlank(message = "agentEnhancerCode 不能为空")
    private String agentEnhancerCode;

    /**
     * AgentEnhancer名称
     */
    @NotBlank(message = "agentEnhancerName 不能为空")
    private String agentEnhancerName;

    /**
     * AgentEnhancer类型
     */
    @NotBlank(message = "agentEnhancerType 不能为空")
    private String agentEnhancerType;

    /**
     * 启用状态
     */
    private Boolean enabled;

    /**
     * JSON 字符串（可空）。
     */
    private String configJson;
}

