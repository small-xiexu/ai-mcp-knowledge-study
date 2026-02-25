package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.agent.AgentVersionDraftRequest;
import com.xbk.knowledge.api.dto.agent.AgentVersionPublishRequest;
import com.xbk.knowledge.api.dto.agent.AgentVersionQueryRequest;
import com.xbk.knowledge.api.dto.agent.AgentVersionResponse;
import com.xbk.knowledge.api.dto.agent.AgentVersionRollbackRequest;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;

/**
 * Agent 版本服务接口。
 *
 * 职责：定义 Agent 版本管理的 API 契约。
 *
 * @author sxie
 */
public interface IAgentVersionService {

    /**
     * 按筛选条件分页查询Agent 版本数据。
     *
     * @param request Agent 版本分页查询条件
     * @return AgentVersionResponse 分页数据
     */
    Result<PageResult<AgentVersionResponse>> list(AgentVersionQueryRequest request);

    /**
     * 查询Agent 版本详情。
     *
     * @param request Agent 版本详情查询参数
     * @return AgentVersionResponse 详情
     */
    Result<AgentVersionResponse> get(IdRequest request);

    /**
     * 保存草稿版本。
     *
     * @param request Agent 版本草稿保存参数
     * @return 保存后的 AgentVersionResponse 信息
     */
    Result<AgentVersionResponse> saveDraft(AgentVersionDraftRequest request);

    /**
     * 发布版本。
     *
     * @param request Agent 版本发布参数
     * @return 发布结果
     */
    Result<AgentVersionResponse> publish(AgentVersionPublishRequest request);

    /**
     * 回滚到指定版本。
     *
     * @param request Agent 版本回滚参数
     * @return 回滚结果
     */
    Result<AgentVersionResponse> rollback(AgentVersionRollbackRequest request);
}
