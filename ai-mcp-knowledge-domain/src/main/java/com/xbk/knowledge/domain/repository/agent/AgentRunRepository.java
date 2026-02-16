package com.xbk.knowledge.domain.repository.agent;

import com.xbk.knowledge.domain.model.entity.agent.AgentRun;

import java.util.Optional;

/**
 * AgentRun 仓储接口。
 
  * @author xiexu
  */
public interface AgentRunRepository {

    AgentRun insert(AgentRun run);

    int updateStatusAndMetrics(AgentRun run);

    Optional<AgentRun> findByRunId(Long orgId, String runId);

    /**
     * 更新 run 状态（可选写入 errorMessage/endedAt）。
     *
     * @param orgId        组织ID
     * @param runId        运行ID
     * @param status       状态
     * @param errorMessage 错误信息（可空）
     * @param endedAt      结束时间（可空，表示保持未结束）
     * @return 影响行数
     */
    int updateStatus(Long orgId, String runId, String status, String errorMessage, java.time.LocalDateTime endedAt);

    /**
     * 工具调用计数 +N（按 runId + orgId 原子递增）。
     */
    int incrementToolCallCount(String runId, Long orgId, int delta);

    /**
     * 工具拒绝计数 +N（按 runId + orgId 原子递增）。
     */
    int incrementToolDeniedCount(String runId, Long orgId, int delta);
}
