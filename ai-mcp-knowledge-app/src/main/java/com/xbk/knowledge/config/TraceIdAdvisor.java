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
        var traceIdContext = TraceIdUtils.ensureTraceId();
        var traceId = traceIdContext.traceId();
        var generated = traceIdContext.generated();

        var startTime = System.currentTimeMillis();
        var userPrompt = truncatePrompt(getUserText(request), 100);
        log.info("[{}] MCP 请求开始, prompt: {}", traceId, userPrompt);

        try {
            var response = chain.nextCall(request);

            var cost = System.currentTimeMillis() - startTime;
            log.info("[{}] MCP 请求完成, 耗时: {}ms", traceId, cost);

            return response;
        } catch (Exception e) {
            var cost = System.currentTimeMillis() - startTime;
            log.error("[{}] MCP 请求失败, 耗时: {}ms, 错误: {}", traceId, cost, e.getMessage());
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
    private String getUserText(ChatClientRequest request) {
        if (request.prompt().getContents() != null) {
            return request.prompt().getContents();
        }
        return "";
    }

    /**
     * 截断 prompt，避免日志过长
     *
     * @param prompt    原始 prompt
     * @param maxLength 最大长度
     * @return 截断后的 prompt
     */
    private String truncatePrompt(String prompt, int maxLength) {
        if (prompt == null) {
            return "";
        }
        if (prompt.length() <= maxLength) {
            return prompt;
        }
        return prompt.substring(0, maxLength) + "...";
    }

    /**
     * 获取当前 traceId（供外部使用）
     *
     * @return 当前 traceId，如果不存在则生成新的
     */
    public static String getCurrentTraceId() {
        return TraceIdUtils.getOrCreateTraceId();
    }
}
