package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.AgentAppService;
import com.xbk.knowledge.domain.model.entity.agent.Agent;
import com.xbk.knowledge.domain.model.vo.agent.AgentCodeQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentPageQuery;
import com.xbk.knowledge.domain.service.agent.IAgentService;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.context.OrgContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent 控制面应用服务实现。
 
  * @author xiexu
  */
@Service
@RequiredArgsConstructor
public class AgentAppServiceImpl implements AgentAppService {

    private final IAgentService agentService;

    /**
     * queryPage。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public PageResult<Agent> queryPage(AgentPageQuery query) {
        return agentService.queryPage(query);
    }

    /**
     * queryByCode。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public Agent queryByCode(AgentCodeQuery query) {
        return agentService.queryByCode(query);
    }

    /**
     * create。
     *
     * @param agent 参数
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Agent create(Agent agent) {
        return agentService.create(agent);
    }

    /**
     * update。
     *
     * @param agent 参数
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Agent update(Agent agent) {
        return agentService.update(agent);
    }
}

