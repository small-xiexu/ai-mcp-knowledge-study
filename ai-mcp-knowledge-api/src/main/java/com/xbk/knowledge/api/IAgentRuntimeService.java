package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.agent.AgentRuntimeChatRequest;
import com.xbk.knowledge.api.dto.agent.AgentRuntimeInvokeRequest;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.types.contract.PlatformContractV1;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Agent 运行服务接口
 * 定义 Agent 运行入口的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IAgentRuntimeService {

    /**
     * 执行对话调用。
     *
     * @param agentCode Agent 编码
     * @param request Agent 对话调用参数。
     * @return 调用结果
     */
    Result<PlatformContractV1> chat(String agentCode, AgentRuntimeChatRequest request);

    /**
     * 执行流式调用。
     *
     * @param agentCode Agent 编码
     * @param request Agent 流式调用参数。
     * @param httpResponse HTTP 响应对象
     * @return 流式响应对象
     */
    Object stream(String agentCode, AgentRuntimeChatRequest request, HttpServletResponse httpResponse);

    /**
     * 执行通用调用。
     *
     * @param agentCode Agent 编码
     * @param request Agent 运行调用参数。
     * @return 调用结果
     */
    Result<PlatformContractV1> invoke(String agentCode, AgentRuntimeInvokeRequest request);
}
