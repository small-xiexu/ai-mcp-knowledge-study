package com.xbk.knowledge.types.exception;

import lombok.Getter;

/**
 * 工具调用需要审批的异常。
 *
 * 职责：用于中断本次模型执行，将 run 置为 PENDING_APPROVAL，并将审批单信息回传到平台标准结构。
 *
 * @author sxie
 */
@Getter
public class ApprovalRequiredException extends RuntimeException {

    private final Long approvalRequestId;
    private final String toolKey;
    private final String riskLevel;

    /**
     * ApprovalRequired 异常定义。
     *
     * @param approvalRequestId 审批单 ID。
     * @param toolKey 工具标识
     * @param riskLevel 风险等级。
     * @param message 异常消息。
     */
    public ApprovalRequiredException(Long approvalRequestId, String toolKey, String riskLevel, String message) {
        super(message);
        this.approvalRequestId = approvalRequestId;
        this.toolKey = toolKey;
        this.riskLevel = riskLevel;
    }
}

