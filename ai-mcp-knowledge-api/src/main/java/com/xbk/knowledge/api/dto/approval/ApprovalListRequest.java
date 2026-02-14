package com.xbk.knowledge.api.dto.approval;

import lombok.Data;

/**
 * 审批单列表请求（分页）。
 *
 * @author xiexu
 */
@Data
public class ApprovalListRequest {

    /**
     * PENDING/APPROVED/REJECTED/CANCELLED/EXPIRED（可空表示不筛选）。
     */
    private String status;

    private Integer offset = 0;

    private Integer pageSize = 20;
}

