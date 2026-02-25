package com.xbk.knowledge.application.service.armory.factory;

import com.xbk.knowledge.application.service.armory.node.AiClientModelNode;
import com.xbk.knowledge.application.service.armory.node.AiClientNode;
import com.xbk.knowledge.application.service.armory.node.AiClientAgentEnhancerNode;
import com.xbk.knowledge.application.service.armory.node.AiClientToolNode;
import com.xbk.knowledge.application.service.armory.node.RootNode;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * AI 客户端装配工厂（自研节点编排）。
 *
 * @author sxie
 */
@Service
public class DefaultAiClientArmoryStrategyFactory {

    /**
     * ChatClient 缓存键模板。
     */
    private static final String CHAT_CLIENT_CACHE_KEY_TEMPLATE = "%s:%s";

    /**
     * 装配节点链路根节点。
     */
    private final RootNode rootNode;

    /**
     * 标准 ChatClient 缓存表。
     */
    private final ConcurrentMap<String, ChatClient> standardChatClientRegistry = new ConcurrentHashMap<>();

    public DefaultAiClientArmoryStrategyFactory(RootNode rootNode, AiClientToolNode aiClientToolNode, AiClientAgentEnhancerNode aiClientAdvisorNode, AiClientModelNode aiClientModelNode, AiClientNode aiClientNode) {
        rootNode.setNext(aiClientToolNode);
        aiClientToolNode.setNext(aiClientAdvisorNode);
        aiClientAdvisorNode.setNext(aiClientModelNode);
        aiClientModelNode.setNext(aiClientNode);
        this.rootNode = rootNode;
    }

    /**
     * 基于节点链路构建 ChatClient。
     * 
     * @param modelConfig 模型配置
     * @param enableTools 工具开关
     * @param extraAdvisors 额外 AgentEnhancer
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
     * 主动预热标准 ChatClient（无额外 AgentEnhancer）。
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
     * 
     * @return 统计数量。
     */
    public int registrySize() {
        return standardChatClientRegistry.size();
    }

    private ChatClient assembleChatClient(ModelConfig modelConfig, boolean enableTools, CallAdvisor... extraAdvisors) {
        AiClientArmoryContext dynamicContext = AiClientArmoryContext.builder()
                .modelConfig(modelConfig)
                .requestedEnableTools(enableTools)
                .extraAdvisors(extraAdvisors)
                .build();
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

}
