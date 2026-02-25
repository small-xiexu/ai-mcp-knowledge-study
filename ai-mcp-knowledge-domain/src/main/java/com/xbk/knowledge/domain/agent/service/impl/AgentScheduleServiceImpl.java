package com.xbk.knowledge.domain.agent.service.impl;

import com.xbk.knowledge.domain.agent.model.entity.AgentSchedule;
import com.xbk.knowledge.domain.agent.model.valobj.AgentScheduleIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentSchedulePageQuery;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentScheduleRepository;
import com.xbk.knowledge.domain.agent.service.IAgentScheduleService;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AgentSchedule 领域服务实现。
 *
 * 说明：P2 最小集只做调度配置的 CRUD 与启停标记，XXL 联动由应用层编排。
 *
 * @author sxie
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentScheduleServiceImpl implements IAgentScheduleService {
    /**
     * Agent 调度仓储，用于调度配置的增删改查与启停。
     */
    private final AgentScheduleRepository agentScheduleRepository;

    /**
     * 查询Agent 调度。
     *
     * @param query 分页查询条件
     * @return AgentSchedule 分页数据
     */
    @Override
    public PageResult<AgentSchedule> queryPage(AgentSchedulePageQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("query 不能为空");
        }
        int offset = query.offset() == null ? 0 : Math.max(query.offset(), 0);
        int pageSize = query.pageSize() == null ? 20 : Math.min(Math.max(query.pageSize(), 1), 200);
        String scheduleName = StringUtils.hasText(query.scheduleName()) ? query.scheduleName().trim() : null;
        AgentSchedulePageQuery normalized = new AgentSchedulePageQuery(
                query.agentId(),
                scheduleName,
                query.enabled(),
                offset,
                pageSize
        );
        List<AgentSchedule> records = agentScheduleRepository.findPage(normalized);
        long total = agentScheduleRepository.count(normalized);
        int pageNum = (offset / pageSize) + 1;
        return PageResult.of(records, total, pageNum, pageSize);
    }

    /**
     * 查询Agent 调度。
     *
     * @param query 主键查询条件
     * @return AgentSchedule 详情
     */
    @Override
    public AgentSchedule queryById(AgentScheduleIdQuery query) {
        if (query == null || query.id() == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        return agentScheduleRepository.findById(query)
                .orElseThrow(() -> new NotFoundException("调度不存在，id=" + query.id()));
    }

    /**
     * 创建并持久化Agent 调度数据。
     *
     * @param schedule 调度配置
     * @return 创建后的 AgentSchedule 信息
     */
    @Override
    public AgentSchedule create(AgentSchedule schedule) {
        if (schedule == null || schedule.getAgentId() == null) {
            throw new IllegalArgumentException("agentId 不能为空");
        }
        if (!StringUtils.hasText(schedule.getScheduleName())) {
            throw new BusinessException("scheduleName 不能为空");
        }
        if (!StringUtils.hasText(schedule.getCron())) {
            throw new BusinessException("cron 不能为空");
        }
        String scheduleName = schedule.getScheduleName().trim();
        if (agentScheduleRepository.existsByAgentIdAndScheduleName(schedule.getAgentId(), scheduleName, null)) {
            throw new BusinessException("调度名称已存在，agentId=" + schedule.getAgentId() + ", scheduleName=" + scheduleName);
        }
        schedule.setScheduleName(scheduleName);
        schedule.setDescription(StringUtils.hasText(schedule.getDescription()) ? schedule.getDescription().trim() : null);
        LocalDateTime now = LocalDateTime.now();
        if (schedule.getEnabled() == null) {
            schedule.setEnabled(Boolean.TRUE);
        }
        schedule.setCreatedAt(now);
        schedule.setUpdatedAt(now);
        return agentScheduleRepository.insert(schedule);
    }

    /**
     * 更新Agent 调度数据。
     *
     * @param schedule 调度配置
     * @return 更新后的 AgentSchedule 信息
     */
    @Override
    public AgentSchedule update(AgentSchedule schedule) {
        if (schedule == null || schedule.getId() == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        AgentSchedule existed = queryById(new AgentScheduleIdQuery(schedule.getId()));
        if (!existed.getAgentId().equals(schedule.getAgentId())) {
            // 防止越权改绑 agent
            throw new BusinessException("不允许修改 agentId");
        }
        if (!StringUtils.hasText(schedule.getCron())) {
            throw new BusinessException("cron 不能为空");
        }
        if (!StringUtils.hasText(schedule.getScheduleName())) {
            throw new BusinessException("scheduleName 不能为空");
        }
        String scheduleName = schedule.getScheduleName().trim();
        if (agentScheduleRepository.existsByAgentIdAndScheduleName(existed.getAgentId(), scheduleName, existed.getId())) {
            throw new BusinessException("调度名称已存在，agentId=" + existed.getAgentId() + ", scheduleName=" + scheduleName);
        }
        existed.setScheduleName(scheduleName);
        existed.setDescription(StringUtils.hasText(schedule.getDescription()) ? schedule.getDescription().trim() : null);
        existed.setCron(schedule.getCron());
        existed.setPayloadTemplateJson(schedule.getPayloadTemplateJson());
        existed.setUpdatedBy(schedule.getUpdatedBy());
        existed.setUpdatedAt(LocalDateTime.now());
        int affected = agentScheduleRepository.update(existed);
        if (affected <= 0) {
            throw new BusinessException("调度更新失败，id=" + schedule.getId());
        }
        return queryById(new AgentScheduleIdQuery(schedule.getId()));
    }

    /**
     * 启用业务配置。
     *
     * @param id 主键 ID
     * @param operatorId 操作人 ID
     * @return 启用后的 AgentSchedule 信息
     */
    @Override
    public AgentSchedule enable(Long id, Long operatorId) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        queryById(new AgentScheduleIdQuery(id));
        int affected = agentScheduleRepository.updateEnabled(id, true, operatorId);
        if (affected <= 0) {
            throw new BusinessException("启用失败，id=" + id);
        }
        return queryById(new AgentScheduleIdQuery(id));
    }

    /**
     * 禁用业务配置。
     *
     * @param id 主键 ID
     * @param operatorId 操作人 ID
     * @return 禁用后的 AgentSchedule 信息
     */
    @Override
    public AgentSchedule disable(Long id, Long operatorId) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        queryById(new AgentScheduleIdQuery(id));
        int affected = agentScheduleRepository.updateEnabled(id, false, operatorId);
        if (affected <= 0) {
            throw new BusinessException("禁用失败，id=" + id);
        }
        return queryById(new AgentScheduleIdQuery(id));
    }

    /**
     * 绑定调度任务与 XXL-JOB 任务 ID。
     *
     * @param id 主键 ID
     * @param xxlJobId XXL-JOB 任务 ID
     * @param operatorId 操作人 ID
     * @return 绑定后的 AgentSchedule 信息
     */
    @Override
    public AgentSchedule bindXxlJobId(Long id, Long xxlJobId, Long operatorId) {
        if (id == null || xxlJobId == null) {
            throw new IllegalArgumentException("id/xxlJobId 不能为空");
        }
        queryById(new AgentScheduleIdQuery(id));
        int affected = agentScheduleRepository.updateXxlJobId(id, xxlJobId, operatorId);
        if (affected <= 0) {
            throw new BusinessException("回写 xxlJobId 失败，id=" + id);
        }
        return queryById(new AgentScheduleIdQuery(id));
    }

    /**
     * 删除Agent 调度数据。
     * 
     * @param id 主键 ID
     */
    @Override
    public void remove(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        queryById(new AgentScheduleIdQuery(id));
        int affected = agentScheduleRepository.deleteById(id);
        if (affected <= 0) {
            throw new BusinessException("删除失败，id=" + id);
        }
    }
}
