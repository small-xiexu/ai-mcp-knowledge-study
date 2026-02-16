package com.xbk.knowledge.infrastructure.repository.agent;

import com.xbk.knowledge.domain.model.entity.agent.AgentSchedule;
import com.xbk.knowledge.domain.model.vo.agent.AgentScheduleIdQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentSchedulePageQuery;
import com.xbk.knowledge.domain.repository.agent.AgentScheduleRepository;
import com.xbk.knowledge.infrastructure.mapper.agent.AgentScheduleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * AgentSchedule 仓储实现。
 
  * @author xiexu
  */
@Repository
@RequiredArgsConstructor
public class AgentScheduleRepositoryImpl implements AgentScheduleRepository {

    private final AgentScheduleMapper agentScheduleMapper;

    /**
     * findById。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public Optional<AgentSchedule> findById(AgentScheduleIdQuery query) {
        if (query == null || query.orgId() == null || query.id() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(agentScheduleMapper.findById(query));
    }

    /**
     * findByOrgIdAndAgentId。
     *
     * @param orgId 参数
     * @param agentId 参数
     * @return 返回结果
     */
    @Override
    public Optional<AgentSchedule> findByOrgIdAndAgentId(Long orgId, Long agentId) {
        if (orgId == null || agentId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(agentScheduleMapper.findByOrgIdAndAgentId(orgId, agentId));
    }

    /**
     * insert。
     *
     * @param schedule 参数
     * @return 返回结果
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
        agentScheduleMapper.insertSchedule(schedule);
        return schedule;
    }

    /**
     * update。
     *
     * @param schedule 参数
     * @return 返回结果
     */
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

    /**
     * updateEnabled。
     *
     * @param orgId 参数
     * @param id 参数
     * @param enabled 参数
     * @param updatedBy 参数
     * @return 返回结果
     */
    @Override
    public int updateEnabled(Long orgId, Long id, Boolean enabled, Long updatedBy) {
        if (orgId == null || id == null || enabled == null) {
            return 0;
        }
        return agentScheduleMapper.updateEnabled(orgId, id, enabled, updatedBy);
    }

    /**
     * updateXxlJobId。
     *
     * @param orgId 参数
     * @param id 参数
     * @param xxlJobId 参数
     * @param updatedBy 参数
     * @return 返回结果
     */
    @Override
    public int updateXxlJobId(Long orgId, Long id, Long xxlJobId, Long updatedBy) {
        if (orgId == null || id == null || xxlJobId == null) {
            return 0;
        }
        return agentScheduleMapper.updateXxlJobId(orgId, id, xxlJobId, updatedBy);
    }

    /**
     * deleteById。
     *
     * @param orgId 参数
     * @param id 参数
     * @return 返回结果
     */
    @Override
    public int deleteById(Long orgId, Long id) {
        if (orgId == null || id == null) {
            return 0;
        }
        return agentScheduleMapper.deleteById(orgId, id);
    }

    /**
     * count。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public long count(AgentSchedulePageQuery query) {
        if (query == null) {
            return 0;
        }
        return agentScheduleMapper.count(query);
    }

    /**
     * findPage。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public List<AgentSchedule> findPage(AgentSchedulePageQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return agentScheduleMapper.findPage(query);
    }
}

