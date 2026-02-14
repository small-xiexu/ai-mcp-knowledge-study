package com.xbk.knowledge.api.dto.approval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审批单响应 DTO。
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalResponse {

    private Long id;
    private Long orgId;
    private String approvalType;
    private String status;
    private String runId;
    private Long agentId;
    private Long agentVersionId;

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

