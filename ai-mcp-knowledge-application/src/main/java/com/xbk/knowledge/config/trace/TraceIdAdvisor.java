package com.xbk.knowledge.config.trace;

import com.xbk.knowledge.config.ai.GlobalChatAdvisor;
import com.xbk.knowledge.types.trace.TraceIdUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * TraceId 链路追踪 Advisor（全局必备）。
 *
 * 说明：在 ChatClient 调用前自动注入 traceId（写入 MDC），便于端到端串联排障与审计。
 
  * @author xiexu
  */
@Slf4j
@Component
@GlobalChatAdvisor
public class TraceIdAdvisor implements CallAdvisor {

    /**
     * getName。
     *
     * @return 返回结果
     */
    @Override
    public String getName() {
        return "TraceIdAdvisor";
    }

    /**
     * getOrder。
     *
     * @return 返回结果
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * adviseCall。
     *
     * @param request 参数
     * @param chain 参数
     * @return 返回结果
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
     * getCurrentTraceId。
     *
     * @return 返回结果
     */
    public static String getCurrentTraceId() {
        return TraceIdUtils.getOrCreateTraceId();
    }
}

