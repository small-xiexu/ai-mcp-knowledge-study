package com.xbk.knowledge.api.dto.approval;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批单 ID 请求。
 *
 * @author xiexu
 */
@Data
public class ApprovalIdRequest {

    @NotNull(message = "id 不能为空")
    private Long id;
}

