package com.xbk.knowledge.domain.workflow.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Workflow 资产实体（独立于 Agent）。
 *
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workflow {
    private Long id;

    private String workflowCode;

    private String workflowName;

    private String description;

    /**
     * ENABLED/DISABLED。
     */
    private String status;

    private Long currentPublishedVersionId;

    private Long createdBy;

    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

