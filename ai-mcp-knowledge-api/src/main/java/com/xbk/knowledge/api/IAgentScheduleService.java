package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.agent.AgentScheduleCreateRequest;
import com.xbk.knowledge.api.dto.agent.AgentScheduleQueryRequest;
import com.xbk.knowledge.api.dto.agent.AgentScheduleResponse;
import com.xbk.knowledge.api.dto.agent.AgentScheduleUpdateRequest;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;

/**
 * Agent 调度服务接口
 * 定义 Agent 调度任务管理的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IAgentScheduleService {

    /**
     * 分页查询数据列表。
     *
     * @param request 请求参数
     * @return 列表结果
     */
    Result<PageResult<AgentScheduleResponse>> list(AgentScheduleQueryRequest request);

    /**
     * 查询详情信息。
     *
     * @param request 请求参数
     * @return 查询结果
     */
    Result<AgentScheduleResponse> get(IdRequest request);

    /**
     * 创建数据。
     *
     * @param request 请求参数
     * @return 创建结果
     */
    Result<AgentScheduleResponse> create(AgentScheduleCreateRequest request);

    /**
     * 更新数据。
     *
     * @param request 请求参数
     * @return 更新结果
     */
    Result<AgentScheduleResponse> update(AgentScheduleUpdateRequest request);

    /**
     * 启用目标对象。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<AgentScheduleResponse> enable(IdRequest request);

    /**
     * 禁用目标对象。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<AgentScheduleResponse> disable(IdRequest request);

    /**
     * 删除目标对象。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<Void> remove(IdRequest request);
}
