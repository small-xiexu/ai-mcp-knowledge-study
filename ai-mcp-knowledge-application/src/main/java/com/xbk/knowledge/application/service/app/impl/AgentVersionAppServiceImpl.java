package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.AgentVersionAppService;
import com.xbk.knowledge.domain.agent.model.entity.AgentVersion;
import com.xbk.knowledge.domain.agent.model.valobj.AgentVersionIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentVersionPageQuery;
import com.xbk.knowledge.domain.agent.service.IAgentVersionService;
import com.xbk.knowledge.types.common.PageResult;
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
     * @param scopeId 参数
     * @param agentCode 参数
     * @param versionId 参数
     * @param operatorId 参数
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentVersion publish(String agentCode, Long versionId, Long operatorId) {
        return agentVersionService.publish(agentCode, versionId, operatorId);
    }

    /**
     * rollback。
     *
     * @param scopeId 参数
     * @param agentCode 参数
     * @param targetVersionId 参数
     * @param operatorId 参数
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentVersion rollback(String agentCode, Long targetVersionId, Long operatorId) {
        return agentVersionService.rollback(agentCode, targetVersionId, operatorId);
    }
}

