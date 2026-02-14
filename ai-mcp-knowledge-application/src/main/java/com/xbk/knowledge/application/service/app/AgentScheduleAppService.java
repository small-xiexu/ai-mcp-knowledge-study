package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.model.entity.agent.AgentSchedule;
import com.xbk.knowledge.domain.model.vo.agent.AgentScheduleIdQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentSchedulePageQuery;
import com.xbk.knowledge.types.common.PageResult;

/**
 * AgentSchedule 应用服务。
 *
 * 职责：编排调度配置与 XXL-Job 联动用例。
 */
public interface AgentScheduleAppService {

    PageResult<AgentSchedule> queryPage(AgentSchedulePageQuery query);

    AgentSchedule queryById(AgentScheduleIdQuery query);

    AgentSchedule create(AgentSchedule schedule, String agentCode);

    AgentSchedule update(AgentSchedule schedule, String agentCode);

    AgentSchedule enable(Long orgId, Long id);

    AgentSchedule disable(Long orgId, Long id);

    void remove(Long orgId, Long id);
}

