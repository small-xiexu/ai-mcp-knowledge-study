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
    private String runId;

    private Long agentId;

    private String agentCode;

    private Long agentVersionId;

    private String runType;

    private String triggerSource;

    private Long operatorId;

    private String operatorType;

    private Long sessionId;

    /**
     * RUNNING/SUCCESS/FAILED/PENDING_APPROVAL/CANCELLED。
     */
    private String status;

    private Long modelIdUsed;

    private String modelNameUsed;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private Integer toolCallCount;

    private Integer toolDeniedCount;

    private Integer repairAttempts;

    private Long costMs;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;
}
