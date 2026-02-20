package com.xbk.knowledge.api.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Workflow 运行请求。
 *
 * @author sxie
 */
@Data
public class WorkflowRunRequest {

    /**
     * 可选：指定 sessionId（用于对话记忆/上下文）
     */
    private Long sessionId;

    @NotBlank(message = "content 不能为空")
    private String content;

    /**
     * 可选：额外变量（JSON），用于模板渲染/条件判断。
     */
    private String variablesJson;

    /**
     * 可选：指定 versionId；为空时使用当前发布版本。
     */
    private Long workflowVersionId;
}

