package com.xbk.knowledge.config;

import com.xbk.knowledge.types.trace.TraceIdUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * TraceId 链路追踪 Advisor
 * <p>
 * 在 ChatClient 调用前后自动注入 traceId，实现链路追踪。
 * <p>
 * 工作原理：
 * <ol>
 * <li>请求前：生成或获取 traceId，设置到 MDC</li>
 * <li>记录请求日志</li>
 * <li>请求后：记录响应日志和耗时</li>
 * </ol>
 * <p>
 * 注意：traceId 通过 System Prompt 传递给 AI，由 AI 在调用工具时传递给 MCP Server。
 * 需要在 ChatClient 构建时配置 system prompt 包含 traceId 指令。
 *
 * 职责：应用装配配置，用于集中接入框架能力
 * @author xiexu
 * @since 2026-01-26
 */
@Slf4j
@Component
public class TraceIdAdvisor implements CallAdvisor {

    @Override
    public String getName() {
        return "TraceIdAdvisor";
    }

    @Override
    public int getOrder() {
        // 最高优先级，确保 traceId 在其他 Advisor 之前注入
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 生成或获取 traceId
        TraceIdUtils.TraceIdContext traceIdContext = TraceIdUtils.ensureTraceId();
        String traceId = traceIdContext.getTraceId();
        boolean generated = traceIdContext.isGenerated();

        try {
            ChatClientResponse response = chain.nextCall(request);
            return response;
        } catch (Exception e) {
            log.error("[{}] AI 请求失败, 错误: {}", traceId, e.getMessage());
            throw e;
        } finally {
            TraceIdUtils.clearIfGenerated(generated);
        }
    }

    /**
     * 获取用户输入文本
     *
     * @param request 请求
     * @return 用户输入文本
     */
    /**
     * 获取当前 traceId（供外部使用）
     *
     * @return 当前 traceId，如果不存在则生成新的
     */
    public static String getCurrentTraceId() {
        return TraceIdUtils.getOrCreateTraceId();
    }
}
