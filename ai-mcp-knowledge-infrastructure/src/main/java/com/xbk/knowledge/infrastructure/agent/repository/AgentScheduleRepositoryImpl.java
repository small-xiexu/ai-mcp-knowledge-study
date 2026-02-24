package com.xbk.knowledge.infrastructure.agent.repository;

import com.xbk.knowledge.domain.agent.model.entity.AgentSchedule;
import com.xbk.knowledge.domain.agent.model.valobj.AgentScheduleIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentSchedulePageQuery;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentScheduleRepository;
import com.xbk.knowledge.infrastructure.dao.IAgentScheduleDao;
import com.xbk.knowledge.infrastructure.dao.po.AgentSchedulePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AgentSchedule 仓储实现。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class AgentScheduleRepositoryImpl implements AgentScheduleRepository {

    private final IAgentScheduleDao agentScheduleDao;

    /**
     * 查询Agent 调度。
     *
     * @param query 查询条件
     * @return 返回 AgentSchedule 查询结果（可能为空）。
     */
    @Override
    public Optional<AgentSchedule> findById(AgentScheduleIdQuery query) {
        if (query == null || query.id() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(toEntity(agentScheduleDao.findById(query)));
    }

    /**
     * 按智能体ID查询调度配置列表。
     *
     * @param agentId 智能体ID。
     * @return 返回智能体调度配置列表。
     */
    @Override
    public List<AgentSchedule> listByAgentId(Long agentId) {
        if (agentId == null) {
            return Collections.emptyList();
        }
        return agentScheduleDao.listByAgentId(agentId)
                .stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * 判断智能体下调度名称是否已存在。
     *
     * @param agentId 智能体ID。
     * @param scheduleName 调度名称。
     * @param excludeId 排除的记录ID。
     * @return 返回是否满足业务条件。
     */
    @Override
    public boolean existsByAgentIdAndScheduleName(Long agentId, String scheduleName, Long excludeId) {
        if (agentId == null || scheduleName == null || scheduleName.isBlank()) {
            return false;
        }
        return agentScheduleDao.countByAgentIdAndScheduleName(agentId, scheduleName, excludeId) > 0;
    }

    /**
     * 创建并持久化Agent 调度数据。
     *
     * @param schedule 调度配置。
     * @return 返回 AgentSchedule 数据。
     */
    @Override
    public AgentSchedule insert(AgentSchedule schedule) {
        if (schedule == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        if (schedule.getCreatedAt() == null) {
            schedule.setCreatedAt(now);
        }
        if (schedule.getUpdatedAt() == null) {
            schedule.setUpdatedAt(now);
        }
        AgentSchedulePO po = toPO(schedule);
        agentScheduleDao.insertSchedule(po);
        schedule.setId(po.getId());
        return schedule;
    }

    /**
     * 更新Agent 调度数据。
     *
     * @param schedule 调度配置。
     * @return 返回调度更新条数。
     */
    @Override
    public int update(AgentSchedule schedule) {
        if (schedule == null || schedule.getId() == null) {
            return 0;
        }
        if (schedule.getUpdatedAt() == null) {
            schedule.setUpdatedAt(LocalDateTime.now());
        }
        return agentScheduleDao.updateSchedule(toPO(schedule));
    }

    /**
     * 更新Agent 调度数据。
     *
     * @param id 主键 ID
     * @param enabled 启用状态
     * @param updatedBy 更新人 ID
     * @return 返回调度启停更新条数。
     */
    @Override
    public int updateEnabled(Long id, Boolean enabled, Long updatedBy) {
        if (id == null || enabled == null) {
            return 0;
        }
        return agentScheduleDao.updateEnabled(id, enabled, updatedBy);
    }

    /**
     * 更新Agent 调度数据。
     *
     * @param id 主键 ID
     * @param xxlJobId XXL-JOB 任务 ID。
     * @param updatedBy 更新人 ID
     * @return 返回调度绑定更新条数。
     */
    @Override
    public int updateXxlJobId(Long id, Long xxlJobId, Long updatedBy) {
        if (id == null || xxlJobId == null) {
            return 0;
        }
        return agentScheduleDao.updateXxlJobId(id, xxlJobId, updatedBy);
    }

    /**
     * 删除Agent 调度数据。
     *
     * @param id 主键 ID
     * @return 返回调度删除条数。
     */
    @Override
    public int deleteById(Long id) {
        if (id == null) {
            return 0;
        }
        return agentScheduleDao.deleteById(id);
    }

    /**
     * 删除Agent 调度数据。
     *
     * @param agentId Agent ID
     * @return 返回调度删除条数。
     */
    @Override
    public int deleteByAgentId(Long agentId) {
        if (agentId == null) {
            return 0;
        }
        return agentScheduleDao.deleteByAgentId(agentId);
    }

    /**
     * 按条件统计业务数量。
     *
     * @param query 查询条件
     * @return 统计数量
     */
    @Override
    public long count(AgentSchedulePageQuery query) {
        if (query == null) {
            return 0;
        }
        return agentScheduleDao.count(query);
    }

    /**
     * 查询Agent 调度。
     *
     * @param query 查询条件
     * @return 返回 AgentSchedule 列表数据。
     */
    @Override
    public List<AgentSchedule> findPage(AgentSchedulePageQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return agentScheduleDao.findPage(query)
                .stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * 实体转持久化对象。
     */
    private AgentSchedulePO toPO(AgentSchedule schedule) {
        if (schedule == null) {
            return null;
        }
        return AgentSchedulePO.builder()
                .id(schedule.getId())
                .agentId(schedule.getAgentId())
                .agentCode(schedule.getAgentCode())
                .scheduleName(schedule.getScheduleName())
                .description(schedule.getDescription())
                .cron(schedule.getCron())
                .enabled(schedule.getEnabled())
                .xxlJobId(schedule.getXxlJobId())
                .payloadTemplateJson(schedule.getPayloadTemplateJson())
                .createdBy(schedule.getCreatedBy())
                .updatedBy(schedule.getUpdatedBy())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }

    /**
     * 持久化对象转实体。
     */
    private AgentSchedule toEntity(AgentSchedulePO po) {
        if (po == null) {
            return null;
        }
        return AgentSchedule.builder()
                .id(po.getId())
                .agentId(po.getAgentId())
                .agentCode(po.getAgentCode())
                .scheduleName(po.getScheduleName())
                .description(po.getDescription())
                .cron(po.getCron())
                .enabled(po.getEnabled())
                .xxlJobId(po.getXxlJobId())
                .payloadTemplateJson(po.getPayloadTemplateJson())
                .createdBy(po.getCreatedBy())
                .updatedBy(po.getUpdatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
