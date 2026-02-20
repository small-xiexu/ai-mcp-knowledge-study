package com.xbk.knowledge.api.dto.workflow;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * WorkflowVersion 创建请求。
 *
 * @author sxie
 */
@Data
public class WorkflowVersionCreateRequest {

    @NotNull(message = "workflowId 不能为空")
    private Long workflowId;

    private String changeSummary;
}

