package com.xbk.knowledge.domain.agent.model.valobj;

/**
 * AgentSchedule 分页查询参数。
 *
 * @param agentId   Agent ID（可空）
 * @param enabled   启用状态（可空）
 * @param offset    偏移
 * @param pageSize  页大小
 *
 * @author sxie
 */
public record AgentSchedulePageQuery(Long agentId,
        Boolean enabled,
        Integer offset,
        Integer pageSize
) {
}

