package com.xbk.knowledge.domain.agentenhancer.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AgentEnhancer 绑定关系实体。
 *
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentEnhancerBinding {

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * 绑定类型AGENT_VERSION/WORKFLOW_VERSION。
     */
    private String bindType;

    /**
     * 绑定目标 IDAgentVersionId / WorkflowVersionId。
     */
    private Long bindTargetId;

    /**
     * Agent 增强器 ID。
     */
    private Long agentEnhancerId;

    /**
     * 排序序号（越小越先执行）。
     */
    private Integer orderNo;

    /**
     * 是否启用（1启用 0禁用）。
     */
    private Integer enabled;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
