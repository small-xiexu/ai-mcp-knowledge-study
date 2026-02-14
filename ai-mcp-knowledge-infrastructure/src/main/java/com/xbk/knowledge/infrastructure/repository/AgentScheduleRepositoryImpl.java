package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.agent.AgentSchedule;
import com.xbk.knowledge.domain.model.vo.agent.AgentScheduleIdQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentSchedulePageQuery;
import com.xbk.knowledge.domain.repository.AgentScheduleRepository;
import com.xbk.knowledge.infrastructure.mapper.AgentScheduleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * AgentSchedule 仓储实现。
 */
@Repository
@RequiredArgsConstructor
public class AgentScheduleRepositoryImpl implements AgentScheduleRepository {

    private final AgentScheduleMapper agentScheduleMapper;

    @Override
    public Optional<AgentSchedule> findById(AgentScheduleIdQuery query) {
        if (query == null || query.orgId() == null || query.id() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(agentScheduleMapper.findById(query));
    }

    @Override
    public Optional<AgentSchedule> findByOrgIdAndAgentId(Long orgId, Long agentId) {
        if (orgId == null || agentId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(agentScheduleMapper.findByOrgIdAndAgentId(orgId, agentId));
    }

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
        agentScheduleMapper.insertSchedule(schedule);
        return schedule;
    }

    @Override
    public int update(AgentSchedule schedule) {
        if (schedule == null || schedule.getOrgId() == null || schedule.getId() == null) {
            return 0;
        }
        if (schedule.getUpdatedAt() == null) {
            schedule.setUpdatedAt(LocalDateTime.now());
        }
        return agentScheduleMapper.updateSchedule(schedule);
    }

    @Override
    public int updateEnabled(Long orgId, Long id, Boolean enabled, Long updatedBy) {
        if (orgId == null || id == null || enabled == null) {
            return 0;
        }
        return agentScheduleMapper.updateEnabled(orgId, id, enabled, updatedBy);
    }

    @Override
    public int updateXxlJobId(Long orgId, Long id, Long xxlJobId, Long updatedBy) {
        if (orgId == null || id == null || xxlJobId == null) {
            return 0;
        }
        return agentScheduleMapper.updateXxlJobId(orgId, id, xxlJobId, updatedBy);
    }

    @Override
    public int deleteById(Long orgId, Long id) {
        if (orgId == null || id == null) {
            return 0;
        }
        return agentScheduleMapper.deleteById(orgId, id);
    }

    @Override
    public long count(AgentSchedulePageQuery query) {
        if (query == null) {
            return 0;
        }
        return agentScheduleMapper.count(query);
    }

    @Override
    public List<AgentSchedule> findPage(AgentSchedulePageQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return agentScheduleMapper.findPage(query);
    }
}

