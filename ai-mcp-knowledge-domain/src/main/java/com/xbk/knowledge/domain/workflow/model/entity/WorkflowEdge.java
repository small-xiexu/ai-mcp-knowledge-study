package com.xbk.knowledge.domain.workflow.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * WorkflowEdge 实体。
 *
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEdge {

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * Workflow 版本 ID。
     */
    private Long workflowVersionId;

    /**
     * 源节点键。
     */
    private String sourceKey;

    /**
     * 目标节点键。
     */
    private String targetKey;

    /**
     * DEFAULT/TRUE/FALSE/CONDITION
     */
    private String edgeType;

    /**
     * 条件表达式。
     */
    private String conditionExpr;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
