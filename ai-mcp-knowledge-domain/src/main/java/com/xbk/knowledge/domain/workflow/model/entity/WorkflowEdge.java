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
    private Long id;

    private Long workflowVersionId;

    private String sourceKey;

    private String targetKey;

    /**
     * DEFAULT/TRUE/FALSE/CONDITION
     */
    private String edgeType;

    private String conditionExpr;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

