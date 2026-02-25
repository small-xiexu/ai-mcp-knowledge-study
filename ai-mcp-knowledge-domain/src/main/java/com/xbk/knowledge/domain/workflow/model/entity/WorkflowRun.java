package com.xbk.knowledge.domain.workflow.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * WorkflowRun 实体。
 *
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRun {

    /**
     * 运行 ID。
     */
    private String runId;

    /**
     * Workflow ID。
     */
    private Long workflowId;

    /**
     * Workflow 编码。
     */
    private String workflowCode;

    /**
     * Workflow 版本 ID。
     */
    private Long workflowVersionId;

    /**
     * 触发来源。
     */
    private String triggerSource;

    /**
     * 操作人 ID。
     */
    private Long operatorId;

    /**
     * 操作人类型。
     */
    private String operatorType;

    /**
     * 会话 ID。
     */
    private Long sessionId;

    /**
     * RUNNING/SUCCESS/FAILED/PENDING_APPROVAL/CANCELLED。
     */
    private String status;

    /**
     * 当前节点键。
     */
    private String currentNodeKey;

    /**
     * 总耗时（毫秒）。
     */
    private Long costMs;

    /**
     * 错误信息。
     */
    private String errorMessage;

    /**
     * 开始时间。
     */
    private LocalDateTime startedAt;

    /**
     * 结束时间。
     */
    private LocalDateTime endedAt;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
