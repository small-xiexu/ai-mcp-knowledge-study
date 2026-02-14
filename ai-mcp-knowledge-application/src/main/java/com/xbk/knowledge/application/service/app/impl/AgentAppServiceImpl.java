package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.AgentAppService;
import com.xbk.knowledge.domain.model.entity.agent.Agent;
import com.xbk.knowledge.domain.model.vo.agent.AgentCodeQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentPageQuery;
import com.xbk.knowledge.domain.service.IAgentService;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.context.OrgContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent 控制面应用服务实现。
 */
@Service
@RequiredArgsConstructor
public class AgentAppServiceImpl implements AgentAppService {

    private final IAgentService agentService;

    @Override
    public PageResult<Agent> queryPage(AgentPageQuery query) {
        return agentService.queryPage(query);
    }

    @Override
    public Agent queryByCode(AgentCodeQuery query) {
        return agentService.queryByCode(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Agent create(Agent agent) {
        OrgContextHolder.requireExplicitTargetOrgIfSuperAdmin();
        return agentService.create(agent);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Agent update(Agent agent) {
        OrgContextHolder.requireExplicitTargetOrgIfSuperAdmin();
        return agentService.update(agent);
    }
}

