package com.xbk.knowledge.domain.agent.adapter.repository;

import com.xbk.knowledge.domain.agent.model.entity.AgentRun;

import java.util.Optional;

/**
 * AgentRun 仓储接口。
 
  * @author xiexu
  */
public interface AgentRunRepository {

    /**
     * 方法：insert。
     */
    AgentRun insert(AgentRun run);

    /**
     * 方法：updateStatusAndMetrics。
     */
    int updateStatusAndMetrics(AgentRun run);

    /**
     * 方法：findByRunId。
     */
    Optional<AgentRun> findByRunId(String runId);

    /**
     * 更新 run 状态（可选写入 errorMessage/endedAt）。
     *
     * @param scopeId        scopeId
     * @param runId        运行ID
     * @param status       状态
     * @param errorMessage 错误信息（可空）
     * @param endedAt      结束时间（可空，表示保持未结束）
     * @return 影响行数
     */
    int updateStatus(String runId, String status, String errorMessage, java.time.LocalDateTime endedAt);

    /**
     * 工具调用计数 +N（按 runId + scopeId 原子递增）。
     */
    int incrementToolCallCount(String runId,  int delta);

    /**
     * 工具拒绝计数 +N（按 runId + scopeId 原子递增）。
     */
    int incrementToolDeniedCount(String runId,  int delta);
}
