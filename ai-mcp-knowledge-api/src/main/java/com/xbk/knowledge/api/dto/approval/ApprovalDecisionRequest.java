package com.xbk.knowledge.api.dto.approval;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批决策请求（通过/拒绝通用）。
 *
 * @author sxie
 */
@Data
public class ApprovalDecisionRequest {

    /**
     * 主键ID
     */
    @NotNull(message = "id 不能为空")
    private Long id;

    /**
     * 审批意见（可空）。
     */
    private String decisionComment;
}

