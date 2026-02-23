package com.xbk.knowledge.config.trace;

import com.xbk.knowledge.config.ai.GlobalChatAgentEnhancer;
import com.xbk.knowledge.types.trace.TraceIdUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * TraceId 链路追踪 AgentEnhancer（全局必备）。
 *
 * 说明：在 ChatClient 调用前自动注入 traceId（写入 MDC），便于端到端串联排障与审计。
 *
 * @author sxie
 */
@Slf4j
@Component
@GlobalChatAgentEnhancer
public class TraceIdAgentEnhancer implements CallAdvisor {

    /**
     * 返回 AgentEnhancer 名称。
     *
     * @return 返回固定名称标识。
     */
    @Override
    public String getName() {
        return "TraceIdAgentEnhancer";
    }

    /**
     * 返回 AgentEnhancer 执行顺序。
     *
     * @return 返回 AgentEnhancer 执行顺序值。
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * 执行 traceId 注入与清理逻辑。
     *
     * @param request ChatClient 请求参数。
     * @param chain AgentEnhancer 链。
     * @return 返回 ChatClientResponse 数据。
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        TraceIdUtils.TraceIdContext traceIdContext = TraceIdUtils.ensureTraceId();
        String traceId = traceIdContext.getTraceId();
        boolean generated = traceIdContext.isGenerated();
        try {
            return chain.nextCall(request);
        } catch (Exception e) {
            log.error("[{}] AI 请求失败: {}", traceId, e.getMessage());
            throw e;
        } finally {
            TraceIdUtils.clearIfGenerated(generated);
        }
    }

    /**
     * 查询链路追踪 ID。
     *
     * @return 返回当前 traceId。
     */
    public static String getCurrentTraceId() {
        return TraceIdUtils.getOrCreateTraceId();
    }
}
