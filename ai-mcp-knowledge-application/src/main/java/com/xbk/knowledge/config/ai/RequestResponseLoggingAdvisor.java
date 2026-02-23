package com.xbk.knowledge.config.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 请求响应日志 Advisor（可配置）。
 *
 * 说明：用于打印大模型的入参和出参，便于调试与排障。
 *
 * @author sxie
 */
@Slf4j
@Component
public class RequestResponseLoggingAdvisor implements CallAdvisor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 返回 Advisor 名称。
     *
     * @return 返回固定名称标识。
     */
    @Override
    public String getName() {
        return "RequestResponseLoggingAdvisor";
    }

    /**
     * 返回 Advisor 执行顺序。
     *
     * @return 返回 Advisor 执行顺序值。
     */
    @Override
    public int getOrder() {
        // 在 TraceIdAdvisor 之后尽早执行，方便串联排障
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    /**
     * 执行请求与响应日志拦截。
     *
     * @param request ChatClient 请求参数。
     * @param chain Advisor 链。
     * @return 返回 ChatClientResponse 数据。
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        logRequest(request);
        ChatClientResponse response = chain.nextCall(request);
        logResponse(response);
        return response;
    }

    private void logRequest(ChatClientRequest request) {
        try {
            Prompt prompt = request.prompt();
            String promptJson = objectMapper.writeValueAsString(prompt);
            List<?> instructions = prompt.getInstructions();
            int instructionCount = instructions == null ? 0 : instructions.size();
            log.info("[REQUEST] Messages: {}, Prompt: {}", instructionCount, truncate(promptJson, 1000));
        } catch (Exception e) {
            log.warn("打印请求信息失败", e);
        }
    }

    private void logResponse(ChatClientResponse clientResponse) {
        try {
            ChatResponse chatResponse = clientResponse.chatResponse();
            if (chatResponse == null) {
                log.info("[RESPONSE] <empty>");
                return;
            }
            String responseJson = objectMapper.writeValueAsString(chatResponse);
            log.info("[RESPONSE] {}", truncate(responseJson, 1000));
        } catch (Exception e) {
            log.warn("打印响应信息失败", e);
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "null";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "... (truncated)";
    }
}
