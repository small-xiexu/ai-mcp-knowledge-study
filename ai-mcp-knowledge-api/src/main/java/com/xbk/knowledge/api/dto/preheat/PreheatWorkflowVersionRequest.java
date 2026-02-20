package com.xbk.knowledge.api.dto.preheat;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 预热 WorkflowVersion 请求。
 *
 * @author sxie
 */
@Data
public class PreheatWorkflowVersionRequest {

    @NotNull(message = "workflowVersionId 不能为空")
    private Long workflowVersionId;

    /**
     * 是否刷新 MCP 连接与工具缓存（需要 tool:write 权限）。
     */
    private Boolean refreshMcp;
}

