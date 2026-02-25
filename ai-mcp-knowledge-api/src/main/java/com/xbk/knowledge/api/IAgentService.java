package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.agent.AgentCodeRequest;
import com.xbk.knowledge.api.dto.agent.AgentCreateRequest;
import com.xbk.knowledge.api.dto.agent.AgentQueryRequest;
import com.xbk.knowledge.api.dto.agent.AgentResponse;
import com.xbk.knowledge.api.dto.agent.AgentUpdateRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;

/**
 * Agent 管理服务接口。
 *
 * 职责：定义 Agent 控制面管理的 API 契约。
 *
 * @author sxie
 */
public interface IAgentService {

    /**
     * 按筛选条件分页查询智能体数据。
     *
     * @param request 智能体分页查询条件
     * @return 智能体分页数据
     */
    Result<PageResult<AgentResponse>> list(AgentQueryRequest request);

    /**
     * 查询智能体详情。
     *
     * @param request 智能体详情查询参数
     * @return 智能体详情
     */
    Result<AgentResponse> get(AgentCodeRequest request);

    /**
     * 创建智能体数据。
     *
     * @param request 智能体创建参数
     * @return 创建后的智能体信息
     */
    Result<AgentResponse> create(AgentCreateRequest request);

    /**
     * 更新智能体数据。
     *
     * @param request 智能体更新参数
     * @return 更新后的智能体信息
     */
    Result<AgentResponse> update(AgentUpdateRequest request);

    /**
     * 删除智能体。
     *
     * @param request 智能体删除参数
     * @return 删除结果
     */
    Result<Void> remove(AgentCodeRequest request);
}
