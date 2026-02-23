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

    /**
     * binding ID
     */
    private Long bindingId;
    /**
     * 绑定类型
     */
    private String bindType;
    /**
     * 绑定目标ID
     */
    private Long bindTargetId;
    /**
     * AgentEnhancer ID
     */
    private Long agentEnhancerId;
    /**
     * 顺序号
     */
    private Integer orderNo;
    /**
     * binding启用
     */
    private Integer bindingEnabled;
    /**
     * binding创建时间
     */
    private LocalDateTime bindingCreatedAt;
    /**
     * binding更新时间
     */
    private LocalDateTime bindingUpdatedAt;

    /**
     * AgentEnhancer编码
     */
    private String agentEnhancerCode;
    /**
     * AgentEnhancer名称
     */
    private String agentEnhancerName;
    /**
     * AgentEnhancer类型
     */
    private String agentEnhancerType;
    /**
     * AgentEnhancer启用
     */
    private Integer agentEnhancerEnabled;
    /**
     * AgentEnhancerConfig JSON
     */
    private String agentEnhancerConfigJson;
    /**
     * AgentEnhancer创建时间
     */
    private LocalDateTime agentEnhancerCreatedAt;
    /**
     * AgentEnhancer更新时间
     */
    private LocalDateTime agentEnhancerUpdatedAt;
}
