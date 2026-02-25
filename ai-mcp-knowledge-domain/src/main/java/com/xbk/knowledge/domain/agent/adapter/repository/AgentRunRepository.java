package com.xbk.knowledge.domain.agent.adapter.repository;

import com.xbk.knowledge.domain.agent.model.entity.AgentRun;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * AgentRun 仓储接口。
 *
 * @author sxie
 */
public interface AgentRunRepository {

    /**
     * 新增记录。
     * 
     * @param run 待新增的运行记录实体。
     */
    void insert(AgentRun run);

    /**
     * 更新执行状态与指标数据。
     * 
     * @param run 包含状态与指标数据的运行记录实体。
     */
    void updateStatusAndMetrics(AgentRun run);

    /**
     * 按运行 ID 查询记录。
     * 
     * @param runId 运行 ID。
     * @return 可选的运行记录实体。
     */
    Optional<AgentRun> findByRunId(String runId);

    /**
     * 更新 run 状态（可选写入 errorMessage/endedAt）。
     * 
     * @param runId 运行ID
     * @param status 状态
     * @param errorMessage 错误信息（可空）
     * @param endedAt 结束时间（可空，表示保持未结束）
     * @return 影响行数
     */
    int updateStatus(String runId, String status, String errorMessage, LocalDateTime endedAt);

    /**
     * 工具调用计数 +N（按 runId 原子递增）。
     * 
     * @param runId 运行 ID。
     * @param delta 增量值。
     * @return 影响行数。
     */
    int incrementToolCallCount(String runId, int delta);

    /**
     * 工具拒绝计数 +N（按 runId 原子递增）。
     * 
     * @param runId 运行 ID。
     * @param delta 增量值。
     * @return 影响行数。
     */
    int incrementToolDeniedCount(String runId, int delta);

    /**
     * 删除指定 Agent 关联记录。
     * 
     * @param agentId 智能体 ID。
     * @return 影响行数。
     */
    int deleteByAgentId(Long agentId);
}
