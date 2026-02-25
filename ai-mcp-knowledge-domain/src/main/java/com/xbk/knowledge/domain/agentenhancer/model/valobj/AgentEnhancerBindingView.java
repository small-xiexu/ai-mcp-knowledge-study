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

    /**
     * 绑定记录 ID。
     */
    private Long bindingId;

    /**
     * 绑定类型。
     */
    private String bindType;

    /**
     * 绑定目标 ID。
     */
    private Long bindTargetId;

    /**
     * Agent 增强器 ID。
     */
    private Long agentEnhancerId;

    /**
     * 绑定排序号。
     */
    private Integer orderNo;

    /**
     * 绑定启用状态。
     */
    private Integer bindingEnabled;

    /**
     * 绑定创建时间。
     */
    private LocalDateTime bindingCreatedAt;

    /**
     * 绑定更新时间。
     */
    private LocalDateTime bindingUpdatedAt;

    /**
     * Agent 增强器编码。
     */
    private String agentEnhancerCode;

    /**
     * Agent 增强器名称。
     */
    private String agentEnhancerName;

    /**
     * Agent 增强器类型。
     */
    private String agentEnhancerType;

    /**
     * Agent 增强器启用状态。
     */
    private Integer agentEnhancerEnabled;

    /**
     * Agent 增强器配置 JSON。
     */
    private String agentEnhancerConfigJson;

    /**
     * Agent 增强器创建时间。
     */
    private LocalDateTime agentEnhancerCreatedAt;

    /**
     * Agent 增强器更新时间。
     */
    private LocalDateTime agentEnhancerUpdatedAt;
}
