package com.xbk.knowledge.domain.workflow.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Workflow 运行上下文快照（用于审批后续跑）。
 *
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRunContext {
    private Long id;

    private String runId;

    /**
     * SAVED/RESUMED/EXPIRED。
     */
    private String status;

    private String snapshotJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

