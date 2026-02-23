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
 *
 * @author sxie
 */
@Service
@RequiredArgsConstructor
public class AgentVersionAppServiceImpl implements AgentVersionAppService {

    private final IAgentVersionService agentVersionService;

    /**
     * 查询Agent 版本。
     *
     * @param query 查询条件
     * @return 返回 AgentVersion 分页数据。
     */
    @Override
    public PageResult<AgentVersion> queryPage(AgentVersionPageQuery query) {
        return agentVersionService.queryPage(query);
    }

    /**
     * 查询Agent 版本。
     *
     * @param query 查询条件
     * @return 返回 AgentVersion 数据。
     */
    @Override
    public AgentVersion queryById(AgentVersionIdQuery query) {
        return agentVersionService.queryById(query);
    }

    /**
     * 创建并持久化Agent 版本数据。
     *
     * @param draft 草稿版本实体。
     * @return Agent 版本保存结果。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentVersion createDraft(AgentVersion draft) {
        return agentVersionService.createDraft(draft);
    }

    /**
     * 更新Agent 版本数据。
     *
     * @param draft 草稿版本实体。
     * @return Agent 版本更新结果。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentVersion updateDraft(AgentVersion draft) {
        return agentVersionService.updateDraft(draft);
    }

    /**
     * 发布业务配置。
     *
     * @param agentCode Agent 编码
     * @param versionId 版本 ID
     * @param operatorId 操作人 ID
     * @return 返回 AgentVersion 数据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentVersion publish(String agentCode, Long versionId, Long operatorId) {
        return agentVersionService.publish(agentCode, versionId, operatorId);
    }

    /**
     * 回滚业务配置。
     *
     * @param agentCode Agent 编码
     * @param targetVersionId 目标版本 ID。
     * @param operatorId 操作人 ID
     * @return 返回 AgentVersion 数据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentVersion rollback(String agentCode, Long targetVersionId, Long operatorId) {
        return agentVersionService.rollback(agentCode, targetVersionId, operatorId);
    }
}

