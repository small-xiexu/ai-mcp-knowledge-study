package com.xbk.knowledge.api.dto.agentenhancer;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AgentEnhancer 绑定视图响应。
 *
 * @author sxie
 */
@Data
public class AgentEnhancerBindingViewResponse {

    private Long bindingId;
    private String bindType;
    private Long bindTargetId;
    private Long agentEnhancerId;
    private Integer orderNo;
    private Integer bindingEnabled;
    private LocalDateTime bindingCreatedAt;
    private LocalDateTime bindingUpdatedAt;

    private String agentEnhancerCode;
    private String agentEnhancerName;
    private String agentEnhancerType;
    private Integer agentEnhancerEnabled;
    private String agentEnhancerConfigJson;
    private LocalDateTime agentEnhancerCreatedAt;
    private LocalDateTime agentEnhancerUpdatedAt;
}
