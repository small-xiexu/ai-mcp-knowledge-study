package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.agent.model.entity.AgentSchedule;
import com.xbk.knowledge.domain.agent.model.valobj.AgentScheduleIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentSchedulePageQuery;
import com.xbk.knowledge.types.common.PageResult;

/**
 * AgentSchedule 应用服务。
 *
 * 职责：编排调度配置与 XXL-Job 联动用例。
 *
 * @author sxie
 */
public interface AgentScheduleAppService {

    PageResult<AgentSchedule> queryPage(AgentSchedulePageQuery query);

    AgentSchedule queryById(AgentScheduleIdQuery query);

    AgentSchedule create(AgentSchedule schedule, String agentCode);

    AgentSchedule update(AgentSchedule schedule, String agentCode);

    AgentSchedule enable(Long id);

    AgentSchedule disable(Long id);

    void remove(Long id);
}

