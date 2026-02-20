package com.xbk.knowledge.domain.workflow.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * WorkflowRun 实体。
 *
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRun {
    private String runId;

    private Long workflowId;

    private String workflowCode;

    private Long workflowVersionId;

    private String triggerSource;

    private Long operatorId;

    private String operatorType;

    private Long sessionId;

    /**
     * RUNNING/SUCCESS/FAILED/PENDING_APPROVAL/CANCELLED。
     */
    private String status;

    private String currentNodeKey;

    private Long costMs;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

