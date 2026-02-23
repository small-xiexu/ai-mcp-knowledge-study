package com.xbk.knowledge.domain.agent.adapter.repository;

import com.xbk.knowledge.domain.agent.model.entity.AgentRunContext;

import java.util.Optional;

/**
 * AgentRunContext 仓储接口。
 *
 * 职责：保存可恢复的运行上下文快照，用于“方式B”审批通过后自动续跑。
 *
 * @author sxie
 */
public interface AgentRunContextRepository {

    /**
     * 按运行维度新增或更新上下文。
     */
    void upsert(AgentRunContext context);

    /**
     * 按运行 ID 查询记录。
     */
    Optional<AgentRunContext> findByRunId(String runId);

    /**
     * 更新执行状态及异常信息。
     */
    int updateStatus(String runId, String status);

    /**
     * 删除指定 Agent 关联记录。
     */
    int deleteByAgentId(Long agentId);
}
