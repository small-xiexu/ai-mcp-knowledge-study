package com.xbk.knowledge.domain.approval.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 审批单实体。
 * 对应数据库表：approval_request
 *
 * 职责：承载高风险工具调用的审批流程信息。
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest {
    private Long id;

    private String approvalType;

    private String status;

    private String runId;

    private Long agentId;

    private Long agentVersionId;

    private Long workflowId;

    private Long workflowVersionId;

    /**
     * 触发审批的节点 key（Workflow 场景）。
     */
    private String nodeKey;

    private Long requesterId;

    private String requesterType;

    private String requestReason;

    private Long approverId;

    private String decisionComment;

    private LocalDateTime decidedAt;

    private String toolKey;

    private String riskLevel;

    private String argumentsSnapshotJson;

    private String argumentsDigest;

    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
