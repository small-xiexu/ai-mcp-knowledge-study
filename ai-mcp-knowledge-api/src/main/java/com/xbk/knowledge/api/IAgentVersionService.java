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
 * Agent 版本服务接口
 * 定义 Agent 版本管理的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IAgentVersionService {

    /**
     * 分页查询数据列表。
     *
     * @param request 请求参数
     * @return 列表结果
     */
    Result<PageResult<AgentVersionResponse>> list(AgentVersionQueryRequest request);

    /**
     * 查询详情信息。
     *
     * @param request 请求参数
     * @return 查询结果
     */
    Result<AgentVersionResponse> get(IdRequest request);

    /**
     * 保存草稿版本。
     *
     * @param request 请求参数
     * @return 保存结果
     */
    Result<AgentVersionResponse> saveDraft(AgentVersionDraftRequest request);

    /**
     * 发布版本。
     *
     * @param request 请求参数
     * @return 发布结果
     */
    Result<AgentVersionResponse> publish(AgentVersionPublishRequest request);

    /**
     * 回滚到指定版本。
     *
     * @param request 请求参数
     * @return 回滚结果
     */
    Result<AgentVersionResponse> rollback(AgentVersionRollbackRequest request);
}
