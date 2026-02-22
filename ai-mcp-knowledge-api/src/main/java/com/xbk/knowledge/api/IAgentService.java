package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.agent.AgentCodeRequest;
import com.xbk.knowledge.api.dto.agent.AgentCreateRequest;
import com.xbk.knowledge.api.dto.agent.AgentQueryRequest;
import com.xbk.knowledge.api.dto.agent.AgentResponse;
import com.xbk.knowledge.api.dto.agent.AgentUpdateRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;

/**
 * Agent 管理服务接口
 * 定义 Agent 控制面管理的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IAgentService {

    /**
     * 分页查询数据列表。
     *
     * @param request 请求参数
     * @return 列表结果
     */
    Result<PageResult<AgentResponse>> list(AgentQueryRequest request);

    /**
     * 查询详情信息。
     *
     * @param request 请求参数
     * @return 查询结果
     */
    Result<AgentResponse> get(AgentCodeRequest request);

    /**
     * 创建数据。
     *
     * @param request 请求参数
     * @return 创建结果
     */
    Result<AgentResponse> create(AgentCreateRequest request);

    /**
     * 更新数据。
     *
     * @param request 请求参数
     * @return 更新结果
     */
    Result<AgentResponse> update(AgentUpdateRequest request);

    /**
     * 删除目标对象。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<Void> remove(AgentCodeRequest request);
}
