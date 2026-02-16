package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.AgentVersionAppService;
import com.xbk.knowledge.domain.model.entity.agent.AgentVersion;
import com.xbk.knowledge.domain.model.vo.agent.AgentVersionIdQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentVersionPageQuery;
import com.xbk.knowledge.domain.service.agent.IAgentVersionService;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.context.OrgContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AgentVersion 控制面应用服务实现。
 
  * @author xiexu
  */
@Service
@RequiredArgsConstructor
public class AgentVersionAppServiceImpl implements AgentVersionAppService {

    private final IAgentVersionService agentVersionService;

    /**
     * queryPage。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public PageResult<AgentVersion> queryPage(AgentVersionPageQuery query) {
        return agentVersionService.queryPage(query);
    }

    /**
     * queryById。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public AgentVersion queryById(AgentVersionIdQuery query) {
        return agentVersionService.queryById(query);
    }

    /**
     * createDraft。
     *
     * @param draft 参数
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentVersion createDraft(AgentVersion draft) {
        return agentVersionService.createDraft(draft);
    }

    /**
     * updateDraft。
     *
     * @param draft 参数
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentVersion updateDraft(AgentVersion draft) {
        return agentVersionService.updateDraft(draft);
    }

    /**
     * publish。
     *
     * @param orgId 参数
     * @param agentCode 参数
     * @param versionId 参数
     * @param operatorId 参数
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentVersion publish(Long orgId, String agentCode, Long versionId, Long operatorId) {
        return agentVersionService.publish(orgId, agentCode, versionId, operatorId);
    }

    /**
     * rollback。
     *
     * @param orgId 参数
     * @param agentCode 参数
     * @param targetVersionId 参数
     * @param operatorId 参数
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentVersion rollback(Long orgId, String agentCode, Long targetVersionId, Long operatorId) {
        return agentVersionService.rollback(orgId, agentCode, targetVersionId, operatorId);
    }
}

