package com.xbk.knowledge.api.dto.workflow;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Workflow 节点运行明细响应。
 *
 * @author xiexu
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
