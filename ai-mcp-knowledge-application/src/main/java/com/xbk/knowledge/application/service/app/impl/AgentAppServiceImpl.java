package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.AgentAppService;
import com.xbk.knowledge.application.service.app.XxlJobAppService;
import com.xbk.knowledge.domain.agent.model.entity.Agent;
import com.xbk.knowledge.domain.agent.model.entity.AgentSchedule;
import com.xbk.knowledge.domain.agent.model.valobj.AgentCodeQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentPageQuery;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentScheduleRepository;
import com.xbk.knowledge.domain.agent.service.IAgentService;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.common.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Agent 控制面应用服务实现。
 *
 * @author sxie
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentAppServiceImpl implements AgentAppService {

    private final IAgentService agentService;
    private final AgentScheduleRepository agentScheduleRepository;
    private final XxlJobAppService xxlJobAppService;

    /**
     * 查询智能体。
     *
     * @param query 查询条件
     * @return 返回 Agent 分页数据。
     */
    @Override
    public PageResult<Agent> queryPage(AgentPageQuery query) {
        return agentService.queryPage(query);
    }

    /**
     * 查询智能体。
     *
     * @param query 查询条件
     * @return 返回 Agent 数据。
     */
    @Override
    public Agent queryByCode(AgentCodeQuery query) {
        return agentService.queryByCode(query);
    }

    /**
     * 创建并持久化智能体数据。
     *
     * @param agent 智能体实体。
     * @return 智能体保存结果。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Agent create(Agent agent) {
        return agentService.create(agent);
    }

    /**
     * 更新智能体数据。
     *
     * @param agent 智能体实体。
     * @return 智能体更新结果。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Agent update(Agent agent) {
        return agentService.update(agent);
    }

    /**
     * 删除智能体数据。
     *
     * @param query 查询条件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(AgentCodeQuery query) {
        Agent agent = agentService.queryByCode(query);
        if (agent != null && agent.getId() != null) {
            List<AgentSchedule> schedules = agentScheduleRepository.listByAgentId(agent.getId());
            for (AgentSchedule schedule : schedules) {
                if (schedule == null || schedule.getXxlJobId() == null) {
                    continue;
                }
                try {
                    xxlJobAppService.removeJob(schedule.getXxlJobId());
                } catch (Exception e) {
                    log.error("删除 Agent 关联 xxl-job 失败，agentCode={}, scheduleId={}, xxlJobId={}",
                            query.getAgentCode(), schedule.getId(), schedule.getXxlJobId(), e);
                    throw new BusinessException("删除 Agent 失败：XXL 任务清理失败，scheduleId=" + schedule.getId());
                }
            }
        }
        agentService.remove(query);
    }
}
