package com.xbk.knowledge.domain.model.vo.agent;

/**
 * AgentSchedule 分页查询参数。
 *
 * @param orgId     组织ID
 * @param agentId   Agent ID（可空）
 * @param enabled   启用状态（可空）
 * @param offset    偏移
 * @param pageSize  页大小
 
  * @author xiexu
  */
public record AgentSchedulePageQuery(
        Long orgId,
        Long agentId,
        Boolean enabled,
        Integer offset,
        Integer pageSize
) {
}

