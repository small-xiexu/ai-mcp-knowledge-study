package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.AgentVersionAppService;
import com.xbk.knowledge.domain.model.entity.agent.AgentVersion;
import com.xbk.knowledge.domain.model.vo.agent.AgentVersionIdQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentVersionPageQuery;
import com.xbk.knowledge.domain.service.IAgentVersionService;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.context.OrgContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AgentVersion 控制面应用服务实现。
 */
@Service
@RequiredArgsConstructor
public class AgentVersionAppServiceImpl implements AgentVersionAppService {

    private final IAgentVersionService agentVersionService;

    @Override
    public PageResult<AgentVersion> queryPage(AgentVersionPageQuery query) {
        return agentVersionService.queryPage(query);
    }

    @Override
    public AgentVersion queryById(AgentVersionIdQuery query) {
        return agentVersionService.queryById(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentVersion createDraft(AgentVersion draft) {
        OrgContextHolder.requireExplicitTargetOrgIfSuperAdmin();
        return agentVersionService.createDraft(draft);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentVersion updateDraft(AgentVersion draft) {
        OrgContextHolder.requireExplicitTargetOrgIfSuperAdmin();
        return agentVersionService.updateDraft(draft);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentVersion publish(Long orgId, String agentCode, Long versionId, Long operatorId) {
        OrgContextHolder.requireExplicitTargetOrgIfSuperAdmin();
        return agentVersionService.publish(orgId, agentCode, versionId, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentVersion rollback(Long orgId, String agentCode, Long targetVersionId, Long operatorId) {
        OrgContextHolder.requireExplicitTargetOrgIfSuperAdmin();
        return agentVersionService.rollback(orgId, agentCode, targetVersionId, operatorId);
    }
}

