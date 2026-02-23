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

    /**
     * 主键ID
     */
    private Long id;
    /**
     * 审批类型
     */
    private String approvalType;
    /**
     * 状态
     */
    private String status;
    /**
     * 运行ID
     */
    private String runId;
    /**
     * Agent ID
     */
    private Long agentId;
    /**
     * Agent 版本ID
     */
    private Long agentVersionId;
    /**
     * Workflow ID
     */
    private Long workflowId;
    /**
     * Workflow 版本ID
     */
    private Long workflowVersionId;
    /**
     * 节点Key
     */
    private String nodeKey;

    /**
     * 申请人ID
     */
    private Long requesterId;
    /**
     * 申请人类型
     */
    private String requesterType;
    /**
     * 申请原因
     */
    private String requestReason;

    /**
     * 审批人ID
     */
    private Long approverId;
    /**
     * 审批意见
     */
    private String decisionComment;
    /**
     * 审批时间
     */
    private LocalDateTime decidedAt;

    /**
     * 工具Key
     */
    private String toolKey;
    /**
     * 风险等级
     */
    private String riskLevel;
    /**
     * 参数摘要
     */
    private String argumentsDigest;
    /**
     * 过期时间
     */
    private LocalDateTime expireAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
