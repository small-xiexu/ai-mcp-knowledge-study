package com.xbk.knowledge.api.dto.workflow;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Workflow 节点运行明细响应 DTO。
 * 定义 Workflow 节点运行明细的 API 返回结构。
 *
 * 职责：接口契约 DTO，用于隔离领域对象并稳定 Trigger 层对外响应。
 *
 * @author sxie
 */
@Data
public class WorkflowNodeRunResponse {

    private Long id;

    private String runId;

    private String nodeKey;

    private String nodeType;

    private String nodeName;

    private String status;

    private Long modelIdUsed;

    private String modelNameUsed;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private Integer toolCallCount;

    private Integer toolDeniedCount;

    private String inputDigest;

    private String outputDigest;

    private String outputText;

    private Integer outputTruncated;

    private Long approvalRequestId;

    private Long costMs;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
