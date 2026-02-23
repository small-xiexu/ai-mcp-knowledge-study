package com.xbk.knowledge.api.dto.workflow;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * WorkflowVersion 列表请求。
 *
 * @author sxie
 */
@Data
public class WorkflowVersionListRequest {

    /**
     * Workflow ID
     */
    @NotNull(message = "workflowId 不能为空")
    private Long workflowId;
}

