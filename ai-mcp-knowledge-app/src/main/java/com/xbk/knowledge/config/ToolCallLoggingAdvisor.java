package com.xbk.knowledge.config;

import com.xbk.knowledge.types.trace.TraceIdUtils;
import com.xbk.knowledge.types.time.TimeCostUtils;
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
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 工具调用日志 Advisor
 * 仅在模型产出工具调用时打印日志，避免普通对话误报为 MCP 调用
 *
 * 职责：应用装配配置，用于集中接入框架能力
 * @author xiexu
 */
@Slf4j
@Component
public class ToolCallLoggingAdvisor implements CallAdvisor {

    @Override
    public String getName() {
        return "ToolCallLoggingAdvisor";
    }

    @Override
    public int getOrder() {
        // 放到链路末端，确保拿到完整响应结果
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        long startTime = TimeCostUtils.start();
        ChatClientResponse response = chain.nextCall(request);
        ChatResponse chatResponse = response == null ? null : response.chatResponse();
        if (hasToolCalls(chatResponse)) {
            String traceId = TraceIdUtils.getOrCreateTraceId();
            long cost = TimeCostUtils.costMillis(startTime);
            String toolNames = extractToolNames(chatResponse);
            String userText = getUserText(request);
            String userPrompt = truncatePrompt(userText, 100);
            log.info("[{}] 工具调用触发, 耗时: {}ms, tools: {}, prompt: {}", traceId, cost, toolNames, userPrompt);
        }
        return response;
    }

    /**
     * 判断是否包含工具调用
     *
     * @param chatResponse 响应
     * @return 是否包含工具调用
     */
    private boolean hasToolCalls(ChatResponse chatResponse) {
        return chatResponse != null && chatResponse.hasToolCalls();
    }

    /**
     * 提取工具名称列表
     *
     * @param chatResponse 响应
     * @return 工具名称
     */
    private String extractToolNames(ChatResponse chatResponse) {
        List<Generation> results = chatResponse.getResults();
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
     * 获取用户输入文本
     *
     * @param request 请求
     * @return 用户输入文本
     */
    private String getUserText(ChatClientRequest request) {
        if (request
                .prompt()
                .getContents() != null) {
            return request
                    .prompt()
                    .getContents();
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
}
