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
 * 请求响应日志 Advisor
 * 用于打印大模型的入参和出参，便于调试
 *
 * 职责：应用装配配置，用于集中接入框架能力
 * @author xiexu
 */
@Slf4j
@Component
public class RequestResponseLoggingAdvisor implements CallAdvisor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 对外暴露 getName 作为调用入口，便于上层复用。
     */
    @Override
    public String getName() {
        return "RequestResponseLoggingAdvisor";
    }

    /**
     * 对外暴露 getOrder 作为调用入口，便于上层复用。
     */
    @Override
    public int getOrder() {
        // 设置为最高优先级，确保最先执行
        return Ordered.HIGHEST_PRECEDENCE + 1;  // 在 TraceIdAdvisor 之后
    }

    /**
     * 对外暴露 adviseCall 作为调用入口，便于上层复用。
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 打印请求信息
        logRequest(request);

        // 调用下一个 Advisor 或实际的模型调用
        ChatClientResponse response = chain.nextCall(request);

        // 打印响应信息
        logResponse(response);

        return response;
    }

    /**
     * 打印请求信息
     */
    private void logRequest(ChatClientRequest request) {
        try {
            Prompt prompt = request.prompt();

            // 整合成一行日志
            String promptJson = objectMapper.writeValueAsString(prompt);
            List<?> instructions = prompt.getInstructions();
            int instructionCount = instructions.size();
            String truncatedPromptJson = truncate(promptJson, 1000);
            log.info("📤 [REQUEST] Messages: {}, Prompt: {}",
                    instructionCount,
                    truncatedPromptJson);
        } catch (Exception e) {
            log.error("打印请求信息失败", e);
        }
    }

    /**
     * 打印响应信息
     */
    private void logResponse(ChatClientResponse clientResponse) {
        try {
            ChatResponse chatResponse = clientResponse.chatResponse();

            if (chatResponse != null) {
                // 整合成一行日志
                String responseJson = objectMapper.writeValueAsString(chatResponse);
                String truncatedResponseJson = truncate(responseJson, 1000);
                log.info("📥 [RESPONSE] {}", truncatedResponseJson);
            } else {
                log.warn("📥 [RESPONSE] ⚠️ 响应为空");
            }
        } catch (Exception e) {
            log.error("打印响应信息失败", e);
        }
    }

    /**
     * 截断长文本
     */
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
