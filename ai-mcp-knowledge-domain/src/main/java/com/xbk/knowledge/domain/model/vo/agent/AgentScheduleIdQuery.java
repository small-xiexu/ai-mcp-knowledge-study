package com.xbk.knowledge.domain.model.vo.agent;

/**
 * AgentSchedule 按 ID 查询参数。
 *
 * @param orgId 组织ID
 * @param id    调度ID
 */
public record AgentScheduleIdQuery(Long orgId, Long id) {
}

