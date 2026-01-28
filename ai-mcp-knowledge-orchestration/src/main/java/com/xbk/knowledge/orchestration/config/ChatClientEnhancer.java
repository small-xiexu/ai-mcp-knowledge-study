package com.xbk.knowledge.orchestration.config;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ChatClient 增强器
 * 统一在编排层注入工具与 Advisor，避免各 Provider 分散装配
 *
 * @author xiexu
 */
@Component
@RequiredArgsConstructor
public class ChatClientEnhancer {

    /**
     * Advisor 列表由 Spring 自动装配
     * 通过统一注入保证链路追踪等横切能力不被遗漏
     */
    private final List<CallAdvisor> advisors;

    /**
     * MCP 工具回调提供者可能缺失
     * 使用可选注入避免启动失败
     */
    @Autowired(required = false)
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * 创建增强后的 ChatClient
     *
     * @param chatModel 具体模型实例
     * @return 增强后的 ChatClient
     */
    public ChatClient enhance(ChatModel chatModel) {
        var builder = ChatClient.builder(chatModel);

        if (toolCallbackProvider != null) {
            builder.defaultToolCallbacks(toolCallbackProvider);
        }

        if (advisors != null && !advisors.isEmpty()) {
            builder.defaultAdvisors(advisors.toArray(new CallAdvisor[0]));
        }

        return builder.build();
    }
}
