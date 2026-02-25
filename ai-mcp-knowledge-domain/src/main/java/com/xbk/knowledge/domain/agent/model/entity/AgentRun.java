package com.xbk.knowledge.domain.agent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AgentRun 实体（运行记录）。
 *
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRun {

    /**
     * 运行 ID。
     */
    private String runId;

    /**
     * Agent ID。
     */
    private Long agentId;

    /**
     * Agent 编码。
     */
    private String agentCode;

    /**
     * Agent 版本 ID。
     */
    private Long agentVersionId;

    /**
     * 运行类型。
     */
    private String runType;

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
     * 使用模型 ID。
     */
    private Long modelIdUsed;

    /**
     * 使用模型名称。
     */
    private String modelNameUsed;

    /**
     * 输入 token 数。
     */
    private Integer promptTokens;

    /**
     * 输出 token 数。
     */
    private Integer completionTokens;

    /**
     * 总 token 数。
     */
    private Integer totalTokens;

    /**
     * 工具调用次数。
     */
    private Integer toolCallCount;

    /**
     * 工具拒绝次数。
     */
    private Integer toolDeniedCount;

    /**
     * 修复尝试次数。
     */
    private Integer repairAttempts;

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
}
