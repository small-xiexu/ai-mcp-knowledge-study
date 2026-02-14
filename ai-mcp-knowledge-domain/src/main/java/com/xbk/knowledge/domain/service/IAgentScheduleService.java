package com.xbk.knowledge.domain.service;

import com.xbk.knowledge.domain.model.entity.agent.AgentSchedule;
import com.xbk.knowledge.domain.model.vo.agent.AgentScheduleIdQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentSchedulePageQuery;
import com.xbk.knowledge.types.common.PageResult;

/**
 * AgentSchedule 领域服务。
 *
 * 职责：封装调度配置的核心业务规则。
 */
public interface IAgentScheduleService {

    PageResult<AgentSchedule> queryPage(AgentSchedulePageQuery query);

    AgentSchedule queryById(AgentScheduleIdQuery query);

    AgentSchedule create(AgentSchedule schedule);

    AgentSchedule update(AgentSchedule schedule);

    AgentSchedule enable(Long orgId, Long id, Long operatorId);

    AgentSchedule disable(Long orgId, Long id, Long operatorId);

    /**
     * 绑定/回写关联的 XXL Job ID。
     */
    AgentSchedule bindXxlJobId(Long orgId, Long id, Long xxlJobId, Long operatorId);

    void remove(Long orgId, Long id);
}
