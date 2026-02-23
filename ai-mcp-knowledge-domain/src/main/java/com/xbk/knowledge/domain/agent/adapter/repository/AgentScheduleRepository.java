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
     * 按主键查询记录。
     */
    Optional<AgentSchedule> findById(AgentScheduleIdQuery query);

    /**
     * 查询 Agent 关联记录列表。
     */
    List<AgentSchedule> listByAgentId(Long agentId);

    /**
     * 判断 Agent 下调度名称是否已存在。
     */
    boolean existsByAgentIdAndScheduleName(Long agentId, String scheduleName, Long excludeId);

    /**
     * 新增记录。
     */
    AgentSchedule insert(AgentSchedule schedule);

    /**
     * 更新记录。
     */
    int update(AgentSchedule schedule);

    /**
     * 更新启用状态。
     */
    int updateEnabled(Long id, Boolean enabled, Long updatedBy);

    /**
     * 更新调度关联的 XXL-JOB ID。
     */
    int updateXxlJobId(Long id, Long xxlJobId, Long updatedBy);

    /**
     * 按主键删除记录。
     */
    int deleteById(Long id);

    /**
     * 删除指定 Agent 关联记录。
     */
    int deleteByAgentId(Long agentId);

    /**
     * 统计符合条件的记录数量。
     */
    long count(AgentSchedulePageQuery query);

    /**
     * 按条件分页查询记录。
     */
    List<AgentSchedule> findPage(AgentSchedulePageQuery query);
}
