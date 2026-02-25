package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;

/**
 * ChatClient 组装服务
 * 统一模型选择与 ChatClient 构建
 *
 * 职责：应用服务接口，用于复用模型组装逻辑
 * @author sxie
 */
public interface ChatClientAssemblyService {

    /**
     * 构建默认 ChatClient（使用激活的对话模型）
     * 
     * @param extraAdvisors 额外 Advisor 列表。
     * @return ChatClient
     */
    ChatClient buildDefaultChatClient(CallAdvisor... extraAdvisors);

    /**
     * 基于指定模型配置构建 ChatClient
     * 
     * @param modelConfig 模型配置
     * @param extraAdvisors 额外 Advisor 列表。
     * @return ChatClient
     */
    ChatClient buildChatClient(ModelConfig modelConfig, CallAdvisor... extraAdvisors);

    /**
     * 基于指定模型配置构建 ChatClient（可控制是否注入工具）。
     * 
     * @param modelConfig 模型配置
     * @param enableTools 是否启用工具注入
     * @param extraAdvisors 额外 AgentEnhancers
     * @return ChatClient
     */
    ChatClient buildChatClient(ModelConfig modelConfig, boolean enableTools, CallAdvisor... extraAdvisors);

    /**
     * 基于指定模型配置构建 ChatClient（不注入工具）。
     * 
     * @param modelConfig 模型配置。
     * @param extraAdvisors 额外 Advisor 列表。
     * @return 不包含工具注入的 ChatClient。
     */
    default ChatClient buildChatClientNoTools(ModelConfig modelConfig, CallAdvisor... extraAdvisors) {
        return buildChatClient(modelConfig, false, extraAdvisors);
    }
}
