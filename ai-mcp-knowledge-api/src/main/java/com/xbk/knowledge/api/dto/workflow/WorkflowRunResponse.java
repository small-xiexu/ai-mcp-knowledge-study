package com.xbk.knowledge.api.dto.workflow;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Workflow 运行记录响应 DTO。
 * 定义 Workflow 运行记录的 API 返回结构。
 *
 * 职责：接口契约 DTO，用于隔离领域对象并稳定 Trigger 层对外响应。
 *
 * @author sxie
 */
@Data
public class WorkflowRunResponse {

    private String runId;

    private Long workflowId;

    private String workflowCode;

    private Long workflowVersionId;

    private String triggerSource;

    private Long operatorId;

    private String operatorType;

    private Long sessionId;

    private String status;

    private String currentNodeKey;

    private Long costMs;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
