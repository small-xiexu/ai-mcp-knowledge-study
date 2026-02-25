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
 * Agent 增强器管理服务接口。
 *
 * 职责：定义 Agent 增强器配置与绑定管理的 API 契约。
 *
 * @author sxie
 */
public interface IAgentEnhancerService {

    /**
     * 按筛选条件分页查询 Agent 增强器（AgentEnhancer）数据。
     *
     * @param request Agent 增强器查询条件
     * @return Agent 增强器分页数据
     */
    Result<PageResult<AgentEnhancerResponse>> list(AgentEnhancerQueryRequest request);

    /**
     * 查询 Agent 增强器（AgentEnhancer）详情。
     *
     * @param request Agent 增强器详情查询参数
     * @return Agent 增强器详情
     */
    Result<AgentEnhancerResponse> get(IdRequest request);

    /**
     * 创建或更新 Agent 增强器（AgentEnhancer）。
     *
     * @param request Agent 增强器保存参数
     * @return 保存后的 Agent 增强器信息
     */
    Result<AgentEnhancerResponse> save(AgentEnhancerSaveRequest request);

    /**
     * 启用 Agent 增强器（AgentEnhancer）。
     *
     * @param request Agent 增强器启用参数
     * @return 启用后的 Agent 增强器信息
     */
    Result<AgentEnhancerResponse> enable(IdRequest request);

    /**
     * 禁用 Agent 增强器（AgentEnhancer）。
     *
     * @param request Agent 增强器禁用参数
     * @return 禁用后的 Agent 增强器信息
     */
    Result<AgentEnhancerResponse> disable(IdRequest request);

    /**
     * 删除 Agent 增强器（AgentEnhancer）。
     *
     * @param request Agent 增强器删除参数
     * @return 删除结果
     */
    Result<Void> remove(IdRequest request);

    /**
     * 查询绑定关系列表。
     *
     * @param request 绑定关系查询参数
     * @return 绑定关系视图列表
     */
    Result<List<AgentEnhancerBindingViewResponse>> listBindings(AgentEnhancerBindingGetRequest request);

    /**
     * 保存绑定关系配置。
     *
     * @param request 绑定关系保存参数
     * @return 保存结果
     */
    Result<Void> saveBindings(AgentEnhancerBindingSaveRequest request);
}
