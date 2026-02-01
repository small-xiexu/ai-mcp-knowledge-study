package com.xbk.knowledge.config.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ChatClient 增强器
 * 统一在编排层注入工具与 Advisor，避免各 Provider 分散装配
 *
 * 职责：应用装配配置，用于集中接入框架能力
 * @author xiexu
 */
@Slf4j
@Component
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
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * 对外暴露 ChatClientEnhancer 作为调用入口，便于上层复用。
     */
    public ChatClientEnhancer(List<CallAdvisor> advisors, ObjectProvider<ToolCallbackProvider> toolCallbackProvider) {
        this.advisors = advisors;
        this
                .toolCallbackProvider = toolCallbackProvider
                .getIfAvailable();
    }

    /**
     * 创建增强后的 ChatClient
     *
     * @param chatModel 具体模型实例
     * @return 增强后的 ChatClient
     */
    public ChatClient enhance(ChatModel chatModel) {
        ChatClient.Builder builder = ChatClient.builder(chatModel);

        if (toolCallbackProvider != null) {
            builder.defaultToolCallbacks(toolCallbackProvider);
            String toolProviderName = toolCallbackProvider
                    .getClass()
                    .getName();
            log.info("✅ 已注入 MCP 工具: {}", toolProviderName);
        } else {
            log.warn("⚠️ ToolCallbackProvider 为 null，MCP 工具未注入");
        }

        if (advisors != null && !advisors.isEmpty()) {
            CallAdvisor[] emptyAdvisors = new CallAdvisor[0];
            CallAdvisor[] advisorArray = advisors.toArray(emptyAdvisors);
            builder.defaultAdvisors(advisorArray);
            int advisorCount = advisors.size();
            log.info("✅ 已注入 {} 个 Advisors", advisorCount);
        } else {
            log.warn("⚠️ Advisors 为空");
        }

        return builder.build();
    }
}
