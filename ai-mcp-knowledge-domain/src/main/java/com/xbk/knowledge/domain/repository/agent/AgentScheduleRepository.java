package com.xbk.knowledge.domain.repository.agent;

import com.xbk.knowledge.domain.model.entity.agent.AgentSchedule;
import com.xbk.knowledge.domain.model.vo.agent.AgentScheduleIdQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentSchedulePageQuery;

import java.util.List;
import java.util.Optional;

/**
 * AgentSchedule 仓储。
 *
 * 职责：持久化调度配置（按 org 隔离）。
 
  * @author xiexu
  */
public interface AgentScheduleRepository {

    Optional<AgentSchedule> findById(AgentScheduleIdQuery query);

    Optional<AgentSchedule> findByOrgIdAndAgentId(Long orgId, Long agentId);

    AgentSchedule insert(AgentSchedule schedule);

    int update(AgentSchedule schedule);

    int updateEnabled(Long orgId, Long id, Boolean enabled, Long updatedBy);

    int updateXxlJobId(Long orgId, Long id, Long xxlJobId, Long updatedBy);

    int deleteById(Long orgId, Long id);

    long count(AgentSchedulePageQuery query);

    List<AgentSchedule> findPage(AgentSchedulePageQuery query);
}

