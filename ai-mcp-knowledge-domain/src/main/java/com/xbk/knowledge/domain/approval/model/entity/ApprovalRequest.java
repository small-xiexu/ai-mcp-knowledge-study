package com.xbk.knowledge.domain.approval.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 审批单实体。
 * 对应数据库表approval_request
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

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * 审批类型。
     */
    private String approvalType;

    /**
     * 审批状态。
     */
    private String status;

    /**
     * 关联运行 ID。
     */
    private String runId;

    /**
     * Agent ID。
     */
    private Long agentId;

    /**
     * Agent 版本 ID。
     */
    private Long agentVersionId;

    /**
     * Workflow ID。
     */
    private Long workflowId;

    /**
     * Workflow 版本 ID。
     */
    private Long workflowVersionId;

    /**
     * 触发审批的节点 key（Workflow 场景）。
     */
    private String nodeKey;

    /**
     * 申请人 ID。
     */
    private Long requesterId;

    /**
     * 申请人类型。
     */
    private String requesterType;

    /**
     * 申请原因。
     */
    private String requestReason;

    /**
     * 审批人 ID。
     */
    private Long approverId;

    /**
     * 审批意见。
     */
    private String decisionComment;

    /**
     * 审批时间。
     */
    private LocalDateTime decidedAt;

    /**
     * 工具键。
     */
    private String toolKey;

    /**
     * 风险等级。
     */
    private String riskLevel;

    /**
     * 参数快照 JSON。
     */
    private String argumentsSnapshotJson;

    /**
     * 参数摘要。
     */
    private String argumentsDigest;

    /**
     * 过期时间。
     */
    private LocalDateTime expireAt;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
