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

    /**
     * 运行ID
     */
    private String runId;

    /**
     * Workflow ID
     */
    private Long workflowId;

    /**
     * Workflow 编码
     */
    private String workflowCode;

    /**
     * Workflow 版本ID
     */
    private Long workflowVersionId;

    /**
     * 触发来源
     */
    private String triggerSource;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人类型
     */
    private String operatorType;

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 状态
     */
    private String status;

    /**
     * 当前节点Key
     */
    private String currentNodeKey;

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
