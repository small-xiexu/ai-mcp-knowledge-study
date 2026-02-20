package com.xbk.knowledge.application.service.armory.factory;

import com.xbk.knowledge.application.service.armory.node.AiClientModelNode;
import com.xbk.knowledge.application.service.armory.node.AiClientNode;
import com.xbk.knowledge.application.service.armory.node.AiClientAdvisorNode;
import com.xbk.knowledge.application.service.armory.node.AiClientToolNode;
import com.xbk.knowledge.application.service.armory.node.RootNode;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * AI 客户端装配工厂（自研节点编排）。
 *
 * @author sxie
 */
@Service
public class DefaultAiClientArmoryStrategyFactory {

    private static final String CHAT_CLIENT_CACHE_KEY_TEMPLATE = "%s:%s";

    private final RootNode rootNode;
    private final ConcurrentMap<String, ChatClient> standardChatClientRegistry = new ConcurrentHashMap<>();

    public DefaultAiClientArmoryStrategyFactory(RootNode rootNode, AiClientToolNode aiClientToolNode, AiClientAdvisorNode aiClientAdvisorNode, AiClientModelNode aiClientModelNode, AiClientNode aiClientNode) {
        rootNode.setNext(aiClientToolNode);
        aiClientToolNode.setNext(aiClientAdvisorNode);
        aiClientAdvisorNode.setNext(aiClientModelNode);
        aiClientModelNode.setNext(aiClientNode);
        this.rootNode = rootNode;
    }

    /**
     * 基于节点链路构建 ChatClient。
     *
     * @param modelConfig   模型配置
     * @param enableTools   工具开关
     * @param extraAdvisors 额外 Advisor
     * @return 装配完成的 ChatClient
     */
    public ChatClient chatClient(ModelConfig modelConfig, boolean enableTools, CallAdvisor... extraAdvisors) {
        if (isStandardAssemble(modelConfig, extraAdvisors)) {
            String cacheKey = buildCacheKey(modelConfig.getId(), enableTools);
            return standardChatClientRegistry.computeIfAbsent(cacheKey, key -> assembleChatClient(modelConfig, enableTools, extraAdvisors));
        }
        return assembleChatClient(modelConfig, enableTools, extraAdvisors);
    }

    /**
     * 主动预热标准 ChatClient（无额外 Advisor）。
     *
     * @param modelConfig 模型配置
     * @param enableTools 工具开关
     */
    public void preheat(ModelConfig modelConfig, boolean enableTools) {
        chatClient(modelConfig, enableTools);
    }

    /**
     * 按模型清理标准 ChatClient 缓存。
     *
     * @param modelId 模型 ID
     */
    public void evictModel(Long modelId) {
        if (modelId == null) {
            return;
        }
        String keyPrefix = modelId + ":";
        standardChatClientRegistry.keySet().removeIf(key -> key != null && key.startsWith(keyPrefix));
    }

    /**
     * 获取当前标准 ChatClient 缓存条目数。
     */
    public int registrySize() {
        return standardChatClientRegistry.size();
    }

    private ChatClient assembleChatClient(ModelConfig modelConfig, boolean enableTools, CallAdvisor... extraAdvisors) {
        DynamicContext dynamicContext = DynamicContext.builder().modelConfig(modelConfig).requestedEnableTools(enableTools).extraAdvisors(extraAdvisors).build();
        rootNode.handle(dynamicContext);
        ChatClient chatClient = dynamicContext.getChatClient();
        if (chatClient == null) {
            throw new IllegalStateException("节点编排完成后未生成 ChatClient");
        }
        return chatClient;
    }

    private boolean isStandardAssemble(ModelConfig modelConfig, CallAdvisor... extraAdvisors) {
        return modelConfig != null && modelConfig.getId() != null && (extraAdvisors == null || extraAdvisors.length == 0);
    }

    private String buildCacheKey(Long modelId, boolean enableTools) {
        return CHAT_CLIENT_CACHE_KEY_TEMPLATE.formatted(modelId, enableTools);
    }

    /**
     * 动态上下文，贯穿装配节点链路。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DynamicContext {
        /**
         * 本次装配使用的模型配置。
         */
        private ModelConfig modelConfig;
        /**
         * 调用方请求的工具开关。
         */
        private boolean requestedEnableTools;
        /**
         * 节点链路最终解析后的工具开关。
         */
        private boolean resolvedEnableTools;
        /**
         * 调用方传入的额外 Advisor 列表（可为空）。
         */
        private CallAdvisor[] extraAdvisors;
        /**
         * 框架内置 Advisor 与额外 Advisor 合并后的结果。
         */
        private List<CallAdvisor> mergedAdvisors;
        /**
         * 当前模型可用的工具回调提供器。
         */
        private ToolCallbackProvider toolCallbackProvider;
        /**
         * 基于模型配置构建出的 ChatModel。
         */
        private ChatModel chatModel;
        /**
         * 节点链路最终产出的 ChatClient。
         */
        private ChatClient chatClient;

        /**
         * 归一化 Advisor 数组，避免下游空指针与 null 元素。
         */
        public void normalizeExtraAdvisors() {
            if (extraAdvisors == null || extraAdvisors.length == 0) {
                extraAdvisors = new CallAdvisor[0];
                return;
            }
            extraAdvisors = Arrays.stream(extraAdvisors).filter(Objects::nonNull).toArray(CallAdvisor[]::new);
        }
    }
}
