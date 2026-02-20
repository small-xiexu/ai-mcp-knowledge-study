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
 * 工具调用日志 Advisor（可配置）。
 *
 * 说明：仅在模型产出工具调用时打印日志，避免普通对话误报为 MCP 调用。
 *
 * @author sxie
 */
@Slf4j
@Component
public class ToolCallLoggingAdvisor implements CallAdvisor {

    /**
     * getName。
     *
     * @return 返回结果
     */
    @Override
    public String getName() {
        return "ToolCallLoggingAdvisor";
    }

    /**
     * getOrder。
     *
     * @return 返回结果
     */
    @Override
    public int getOrder() {
        // 放到链路末端，确保拿到完整响应结果
        return Ordered.LOWEST_PRECEDENCE;
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

    private String getUserText(ChatClientRequest request) {
        if (request == null || request.prompt() == null) {
            return "";
        }
        if (request.prompt().getContents() != null) {
            return request.prompt().getContents();
        }
        return "";
    }

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
