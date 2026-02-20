package com.xbk.knowledge.api.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审批单响应 DTO。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalResponse {

    private Long id;
    private String approvalType;
    private String status;
    private String runId;
    private Long agentId;
    private Long agentVersionId;
    private Long workflowId;
    private Long workflowVersionId;
    private String nodeKey;

    private Long requesterId;
    private String requesterType;
    private String requestReason;

    private Long approverId;
    private String decisionComment;
    private LocalDateTime decidedAt;

    private String toolKey;
    private String riskLevel;
    private String argumentsDigest;
    private LocalDateTime expireAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
