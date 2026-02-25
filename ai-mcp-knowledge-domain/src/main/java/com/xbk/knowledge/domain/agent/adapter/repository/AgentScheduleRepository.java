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
     * 
     * @param query 主键查询条件。
     * @return 可选的调度配置实体。
     */
    Optional<AgentSchedule> findById(AgentScheduleIdQuery query);

    /**
     * 查询 Agent 关联记录列表。
     * 
     * @param agentId 智能体 ID。
     * @return 调度配置列表。
     */
    List<AgentSchedule> listByAgentId(Long agentId);

    /**
     * 判断 Agent 下调度名称是否已存在。
     * 
     * @param agentId 智能体 ID。
     * @param scheduleName 调度名称。
     * @param excludeId 排除的 ID。
     * @return `true` 表示已存在重名调度，`false` 表示不存在。
     */
    boolean existsByAgentIdAndScheduleName(Long agentId, String scheduleName, Long excludeId);

    /**
     * 新增记录。
     * 
     * @param schedule 待新增的调度配置实体。
     * @return 已持久化的调度配置实体。
     */
    AgentSchedule insert(AgentSchedule schedule);

    /**
     * 更新记录。
     * 
     * @param schedule 待更新的调度配置实体。
     * @return 影响行数。
     */
    int update(AgentSchedule schedule);

    /**
     * 更新启用状态。
     * 
     * @param id 主键 ID。
     * @param enabled 启用标识。
     * @param updatedBy 操作人标识。
     * @return 影响行数。
     */
    int updateEnabled(Long id, Boolean enabled, Long updatedBy);

    /**
     * 更新调度关联的 XXL-JOB ID。
     * 
     * @param id 主键 ID。
     * @param xxlJobId XXL 任务 ID。
     * @param updatedBy 操作人标识。
     * @return 影响行数。
     */
    int updateXxlJobId(Long id, Long xxlJobId, Long updatedBy);

    /**
     * 按主键删除记录。
     * 
     * @param id 主键 ID。
     * @return 影响行数。
     */
    int deleteById(Long id);

    /**
     * 删除指定 Agent 关联记录。
     * 
     * @param agentId 智能体 ID。
     * @return 影响行数。
     */
    int deleteByAgentId(Long agentId);

    /**
     * 统计符合条件的记录数量。
     * 
     * @param query 分页查询条件。
     * @return 统计数量。
     */
    long count(AgentSchedulePageQuery query);

    /**
     * 按条件分页查询记录。
     * 
     * @param query 分页查询条件。
     * @return 调度配置分页列表。
     */
    List<AgentSchedule> findPage(AgentSchedulePageQuery query);
}
