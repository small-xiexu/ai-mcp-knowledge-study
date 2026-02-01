package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.model.entity.ModelConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;

/**
 * ChatClient 组装服务
 * 统一模型选择与 ChatClient 构建
 *
 * 职责：应用服务接口，用于复用模型组装逻辑
 * @author xiexu
 */
public interface ChatClientAssemblyService {

    /**
     * 构建默认 ChatClient（使用激活的对话模型）
     *
     * @return ChatClient
     */
    ChatClient buildDefaultChatClient(CallAdvisor... extraAdvisors);

    /**
     * 基于指定模型配置构建 ChatClient
     *
     * @param modelConfig 模型配置
     * @return ChatClient
     */
    ChatClient buildChatClient(ModelConfig modelConfig, CallAdvisor... extraAdvisors);
}
