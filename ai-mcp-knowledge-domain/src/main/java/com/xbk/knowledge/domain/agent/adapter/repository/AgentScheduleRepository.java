package com.xbk.knowledge.domain.agent.adapter.repository;

import com.xbk.knowledge.domain.agent.model.entity.AgentSchedule;
import com.xbk.knowledge.domain.agent.model.valobj.AgentScheduleIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentSchedulePageQuery;

import java.util.List;
import java.util.Optional;

/**
 * AgentSchedule 仓储。
 *
 * 职责：持久化调度配置。
 *
 * @author sxie
 */
public interface AgentScheduleRepository {

    /**
     * 方法：findById。
     */
    Optional<AgentSchedule> findById(AgentScheduleIdQuery query);

    /**
     * 方法：listByAgentId。
     */
    List<AgentSchedule> listByAgentId(Long agentId);

    /**
     * 方法：existsByAgentIdAndScheduleName。
     */
    boolean existsByAgentIdAndScheduleName(Long agentId, String scheduleName, Long excludeId);

    /**
     * 方法：insert。
     */
    AgentSchedule insert(AgentSchedule schedule);

    /**
     * 方法：update。
     */
    int update(AgentSchedule schedule);

    /**
     * 方法：updateEnabled。
     */
    int updateEnabled(Long id, Boolean enabled, Long updatedBy);

    /**
     * 方法：updateXxlJobId。
     */
    int updateXxlJobId(Long id, Long xxlJobId, Long updatedBy);

    /**
     * 方法：deleteById。
     */
    int deleteById(Long id);

    /**
     * 方法：deleteByAgentId。
     */
    int deleteByAgentId(Long agentId);

    /**
     * 方法：count。
     */
    long count(AgentSchedulePageQuery query);

    /**
     * 方法：findPage。
     */
    List<AgentSchedule> findPage(AgentSchedulePageQuery query);
}
