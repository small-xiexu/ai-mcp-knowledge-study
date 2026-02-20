package com.xbk.knowledge.api.dto.workflow;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * WorkflowVersion 发布请求。
 *
 * @author sxie
 */
@Data
public class WorkflowVersionPublishRequest {

    @NotNull(message = "workflowVersionId 不能为空")
    private Long workflowVersionId;
}

