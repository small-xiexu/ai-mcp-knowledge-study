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
     * findById。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public Optional<AgentSchedule> findById(AgentScheduleIdQuery query) {
        if (query == null || query.id() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(toEntity(agentScheduleDao.findById(query)));
    }

    /**
     * findByAgentId。
     *
     * @param agentId 参数
     * @return 返回结果
     */
    @Override
    public Optional<AgentSchedule> findByAgentId(Long agentId) {
        if (agentId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(toEntity(agentScheduleDao.findByAgentId(agentId)));
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
        AgentSchedulePO po = toPO(schedule);
        agentScheduleDao.insertSchedule(po);
        schedule.setId(po.getId());
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
        if (schedule == null || schedule.getId() == null) {
            return 0;
        }
        if (schedule.getUpdatedAt() == null) {
            schedule.setUpdatedAt(LocalDateTime.now());
        }
        return agentScheduleDao.updateSchedule(toPO(schedule));
    }

    /**
     * updateEnabled。
     *
     * @param id 参数
     * @param enabled 参数
     * @param updatedBy 参数
     * @return 返回结果
     */
    @Override
    public int updateEnabled(Long id, Boolean enabled, Long updatedBy) {
        if (id == null || enabled == null) {
            return 0;
        }
        return agentScheduleDao.updateEnabled(id, enabled, updatedBy);
    }

    /**
     * updateXxlJobId。
     *
     * @param id 参数
     * @param xxlJobId 参数
     * @param updatedBy 参数
     * @return 返回结果
     */
    @Override
    public int updateXxlJobId(Long id, Long xxlJobId, Long updatedBy) {
        if (id == null || xxlJobId == null) {
            return 0;
        }
        return agentScheduleDao.updateXxlJobId(id, xxlJobId, updatedBy);
    }

    /**
     * deleteById。
     *
     * @param id 参数
     * @return 返回结果
     */
    @Override
    public int deleteById(Long id) {
        if (id == null) {
            return 0;
        }
        return agentScheduleDao.deleteById(id);
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
        return agentScheduleDao.count(query);
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
