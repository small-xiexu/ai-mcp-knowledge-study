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
     * ApprovalRequiredException。
     *
     * @param approvalRequestId 参数
     * @param toolKey 参数
     * @param riskLevel 参数
     * @param message 参数
     */
    public ApprovalRequiredException(Long approvalRequestId, String toolKey, String riskLevel, String message) {
        super(message);
        this.approvalRequestId = approvalRequestId;
        this.toolKey = toolKey;
        this.riskLevel = riskLevel;
    }
}

