package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.agentenhancer.AgentEnhancerBindingGetRequest;
import com.xbk.knowledge.api.dto.agentenhancer.AgentEnhancerBindingSaveRequest;
import com.xbk.knowledge.api.dto.agentenhancer.AgentEnhancerBindingViewResponse;
import com.xbk.knowledge.api.dto.agentenhancer.AgentEnhancerQueryRequest;
import com.xbk.knowledge.api.dto.agentenhancer.AgentEnhancerResponse;
import com.xbk.knowledge.api.dto.agentenhancer.AgentEnhancerSaveRequest;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;

import java.util.List;

/**
 * Agent 增强器（AgentEnhancer）管理服务接口
 * 定义 Agent 增强器（AgentEnhancer）配置与绑定管理的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IAgentEnhancerService {

    /**
     * 按筛选条件分页查询 Agent 增强器（AgentEnhancer）数据。
     *
     * @param request Agent 增强器（AgentEnhancer）查询条件。
     * @return 返回 AgentEnhancerResponse 分页数据。
     */
    Result<PageResult<AgentEnhancerResponse>> list(AgentEnhancerQueryRequest request);

    /**
     * 查询 Agent 增强器（AgentEnhancer）详情。
     *
     * @param request Agent 增强器（AgentEnhancer）详情查询参数。
     * @return 返回 AgentEnhancerResponse 数据。
     */
    Result<AgentEnhancerResponse> get(IdRequest request);

    /**
     * 创建或更新 Agent 增强器（AgentEnhancer）。
     *
     * @param request Agent 增强器（AgentEnhancer）保存参数。
     * @return 返回 AgentEnhancerResponse 数据。
     */
    Result<AgentEnhancerResponse> save(AgentEnhancerSaveRequest request);

    /**
     * 启用 Agent 增强器（AgentEnhancer）。
     *
     * @param request Agent 增强器（AgentEnhancer）启用参数。
     * @return 启用结果
     */
    Result<AgentEnhancerResponse> enable(IdRequest request);

    /**
     * 禁用 Agent 增强器（AgentEnhancer）。
     *
     * @param request Agent 增强器（AgentEnhancer）禁用参数。
     * @return 禁用结果
     */
    Result<AgentEnhancerResponse> disable(IdRequest request);

    /**
     * 删除 Agent 增强器（AgentEnhancer）。
     *
     * @param request Agent 增强器（AgentEnhancer）删除参数。
     * @return 返回 Agent 增强器（AgentEnhancer）删除状态。
     */
    Result<Void> remove(IdRequest request);

    /**
     * 查询绑定关系列表。
     *
     * @param request 绑定关系查询参数。
     * @return 返回 AgentEnhancerBindingViewResponse 列表数据。
     */
    Result<List<AgentEnhancerBindingViewResponse>> listBindings(AgentEnhancerBindingGetRequest request);

    /**
     * 保存绑定关系配置。
     *
     * @param request 绑定关系保存参数。
     * @return 返回绑定关系保存状态。
     */
    Result<Void> saveBindings(AgentEnhancerBindingSaveRequest request);
}
