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
    private Long id;

    /**
     * 绑定类型：AGENT_VERSION/WORKFLOW_VERSION。
     */
    private String bindType;

    /**
     * 绑定目标 ID：AgentVersionId / WorkflowVersionId。
     */
    private Long bindTargetId;

    private Long agentEnhancerId;

    /**
     * 排序序号（越小越先执行）。
     */
    private Integer orderNo;

    /**
     * 是否启用（1启用 0禁用）。
     */
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

