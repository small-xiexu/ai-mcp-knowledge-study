package com.xbk.knowledge.config.ai;

import com.xbk.knowledge.types.time.TimeCostUtils;
import com.xbk.knowledge.types.trace.TraceIdUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 工具调用日志 AgentEnhancer（可配置）。
 *
 * 说明：仅在模型产出工具调用时打印日志，避免普通对话误报为 MCP 调用。
 *
 * @author sxie
 */
@Slf4j
@Component
public class ToolCallLoggingAgentEnhancer implements CallAdvisor {

    /**
     * 返回 AgentEnhancer 名称。
     *
     * @return 返回固定名称标识。
     */
    @Override
    public String getName() {
        return "ToolCallLoggingAgentEnhancer";
    }

    /**
     * 返回 AgentEnhancer 执行顺序。
     *
     * @return 返回 AgentEnhancer 执行顺序值。
     */
    @Override
    public int getOrder() {
        // 放到链路末端，确保拿到完整响应结果
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * 执行工具调用日志拦截。
     *
     * @param request ChatClient 请求参数。
     * @param chain AgentEnhancer 链。
     * @return 返回 ChatClientResponse 数据。
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        long startTime = TimeCostUtils.start();
        ChatClientResponse response = chain.nextCall(request);
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse != null && chatResponse.hasToolCalls()) {
            String traceId = TraceIdUtils.getOrCreateTraceId();
            long cost = TimeCostUtils.costMillis(startTime);
            String toolNames = extractToolNames(chatResponse);
            String userPrompt = truncatePrompt(getUserText(request), 100);
            log.info("[{}] 工具调用触发, 耗时: {}ms, tools: {}, prompt: {}", traceId, cost, toolNames, userPrompt);
        }
        return response;
    }

    private String extractToolNames(ChatResponse chatResponse) {
        List<Generation> results = chatResponse.getResults();
        if (results == null || results.isEmpty()) {
            return "unknown";
        }
        Function<Generation, AssistantMessage> outputMapper = Generation::getOutput;
        Predicate<AssistantMessage> hasToolCalls = AssistantMessage::hasToolCalls;
        Function<AssistantMessage, Stream<AssistantMessage.ToolCall>> toolCallStreamMapper = message -> message
                .getToolCalls()
                .stream();
        Function<AssistantMessage.ToolCall, String> toolCallNameMapper = AssistantMessage.ToolCall::name;
        Collector<String, ?, Set<String>> collector = Collectors.toSet();
        Set<String> toolNames = results
                .stream()
                .map(outputMapper)
                .filter(Objects::nonNull)
                .filter(hasToolCalls)
                .flatMap(toolCallStreamMapper)
                .map(toolCallNameMapper)
                .collect(collector);
        if (toolNames.isEmpty()) {
            return "unknown";
        }
        return String.join(",", toolNames);
    }

    /**
     * 提取用户输入文本。
     *
     * @param request 请求参数。
     * @return 返回处理后的文本内容。
     */
    private String getUserText(ChatClientRequest request) {
        if (request == null || request.prompt() == null) {
            return "";
        }
        if (request.prompt().getContents() != null) {
            return request.prompt().getContents();
        }
        return "";
    }

    /**
     * 截断过长提示词，避免日志膨胀。
     *
     * @param prompt 提示词。
     * @param maxLength 最大长度。
     * @return 返回截断后的提示词文本。
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
}
