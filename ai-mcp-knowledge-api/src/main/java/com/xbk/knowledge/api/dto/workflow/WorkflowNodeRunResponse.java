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

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 运行ID
     */
    private String runId;

    /**
     * 节点Key
     */
    private String nodeKey;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 状态
     */
    private String status;

    /**
     * 模型IDUsed
     */
    private Long modelIdUsed;

    /**
     * 模型名称Used
     */
    private String modelNameUsed;

    /**
     * 提示词Tokens
     */
    private Integer promptTokens;

    /**
     * 补全Tokens
     */
    private Integer completionTokens;

    /**
     * 总数Tokens
     */
    private Integer totalTokens;

    /**
     * 工具调用次数
     */
    private Integer toolCallCount;

    /**
     * 工具拒绝次数
     */
    private Integer toolDeniedCount;

    /**
     * input摘要
     */
    private String inputDigest;

    /**
     * output摘要
     */
    private String outputDigest;

    /**
     * outputText
     */
    private String outputText;

    /**
     * output截断
     */
    private Integer outputTruncated;

    /**
     * 审批请求 ID
     */
    private Long approvalRequestId;

    /**
     * 耗时（毫秒）
     */
    private Long costMs;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 结束时间
     */
    private LocalDateTime endedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
