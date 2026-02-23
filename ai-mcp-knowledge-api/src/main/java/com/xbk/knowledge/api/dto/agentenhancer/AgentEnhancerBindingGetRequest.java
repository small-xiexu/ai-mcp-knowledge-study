package com.xbk.knowledge.api.dto.agentenhancer;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * AgentEnhancer 绑定查询请求。
 *
 * @author sxie
 */
@Data
public class AgentEnhancerBindingGetRequest {

    /**
     * 绑定类型
     */
    @NotBlank(message = "bindType 不能为空")
    private String bindType;

    /**
     * 绑定目标ID
     */
    @NotNull(message = "bindTargetId 不能为空")
    private Long bindTargetId;
}

