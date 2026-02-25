package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.agent.AgentScheduleCreateRequest;
import com.xbk.knowledge.api.dto.agent.AgentScheduleQueryRequest;
import com.xbk.knowledge.api.dto.agent.AgentScheduleResponse;
import com.xbk.knowledge.api.dto.agent.AgentScheduleUpdateRequest;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;

/**
 * Agent 调度服务接口。
 *
 * 职责：定义 Agent 调度任务管理的 API 契约。
 *
 * @author sxie
 */
public interface IAgentScheduleService {

    /**
     * 按筛选条件分页查询Agent 调度数据。
     *
     * @param request Agent 调度分页查询参数
     * @return Agent 调度分页数据
     */
    Result<PageResult<AgentScheduleResponse>> list(AgentScheduleQueryRequest request);

    /**
     * 查询Agent 调度详情。
     *
     * @param request Agent 调度查询参数
     * @return Agent 调度详情
     */
    Result<AgentScheduleResponse> get(IdRequest request);

    /**
     * 创建Agent 调度数据。
     *
     * @param request Agent 调度创建参数
     * @return 创建后的 Agent 调度信息
     */
    Result<AgentScheduleResponse> create(AgentScheduleCreateRequest request);

    /**
     * 更新Agent 调度数据。
     *
     * @param request Agent 调度更新参数
     * @return 更新后的 Agent 调度信息
     */
    Result<AgentScheduleResponse> update(AgentScheduleUpdateRequest request);

    /**
     * 启用 Agent 调度任务。
     *
     * @param request Agent 调度启用参数
     * @return 启用后的 Agent 调度信息
     */
    Result<AgentScheduleResponse> enable(IdRequest request);

    /**
     * 禁用 Agent 调度任务。
     *
     * @param request Agent 调度禁用参数
     * @return 禁用后的 Agent 调度信息
     */
    Result<AgentScheduleResponse> disable(IdRequest request);

    /**
     * 删除 Agent 调度任务。
     *
     * @param request Agent 调度删除参数
     * @return 删除结果
     */
    Result<Void> remove(IdRequest request);
}
