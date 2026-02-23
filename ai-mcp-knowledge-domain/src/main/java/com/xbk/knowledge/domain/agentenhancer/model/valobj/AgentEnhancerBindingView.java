package com.xbk.knowledge.domain.agentenhancer.model.valobj;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AgentEnhancer 绑定视图（用于控制面展示与运行时装配）。
 *
 * 说明：该对象由 join 查询返回，不对应单表实体。
 *
 * @author sxie
 */
@Getter
@Setter
public class AgentEnhancerBindingView {

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

