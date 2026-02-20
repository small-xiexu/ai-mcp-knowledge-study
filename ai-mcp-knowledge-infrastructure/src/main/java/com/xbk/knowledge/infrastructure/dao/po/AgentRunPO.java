package com.xbk.knowledge.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AgentRun 持久化对象。
 *
 * @author sxie
 */
@TableName("agent_run")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRunPO {

    @TableId(value = "run_id")
    private String runId;

    private Long agentId;

    private String agentCode;

    private Long agentVersionId;

    private String runType;

    private String triggerSource;

    private Long operatorId;

    private String operatorType;

    private Long sessionId;

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
