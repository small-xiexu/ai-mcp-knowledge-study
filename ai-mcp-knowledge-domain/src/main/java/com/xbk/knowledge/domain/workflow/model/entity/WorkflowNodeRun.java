package com.xbk.knowledge.domain.workflow.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Workflow 节点运行明细实体。
 *
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowNodeRun {
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

