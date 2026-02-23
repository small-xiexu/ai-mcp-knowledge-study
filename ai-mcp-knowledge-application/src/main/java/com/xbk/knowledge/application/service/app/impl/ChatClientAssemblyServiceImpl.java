package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.armory.factory.DefaultAiClientArmoryStrategyFactory;
import com.xbk.knowledge.application.service.app.ChatClientAssemblyService;
import com.xbk.knowledge.application.service.app.ModelConfigAppService;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.stereotype.Service;

/**
 * ChatClient 组装服务实现
 * 统一模型选择与 ChatClient 构建
 *
 * 职责：应用层用例实现，用于复用模型组装逻辑
 * @author sxie
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatClientAssemblyServiceImpl implements ChatClientAssemblyService {

    private final ModelConfigAppService modelConfigAppService;
    private final DefaultAiClientArmoryStrategyFactory armoryStrategyFactory;

    /**
     * 构建默认 ChatClient（使用激活的对话模型）
     *
     * 为什么：统一入口，避免各业务自行拼装模型与增强器
     * 入参：可选的额外 AgentEnhancer
     * 出参：可直接调用的 ChatClient
     */
    @Override
    public ChatClient buildDefaultChatClient(CallAdvisor... extraAdvisors) {
        ModelConfig activeChatModel = modelConfigAppService.getActiveChatModel();
        if (activeChatModel == null || activeChatModel.getModelType() == null) {
            throw new IllegalStateException("未配置激活的对话模型");
        }
        // 复用统一构建逻辑，保持生成行为一致
        return buildChatClient(activeChatModel, extraAdvisors);
    }

    /**
     * 基于指定模型配置构建 ChatClient
     *
     * 为什么：支持任务或配置指定模型时复用统一装配逻辑
     * 入参：模型配置 + 可选 AgentEnhancer
     * 出参：可直接调用的 ChatClient
     */
    @Override
    public ChatClient buildChatClient(ModelConfig modelConfig, CallAdvisor... extraAdvisors) {
        return buildChatClient(modelConfig, true, extraAdvisors);
    }

    /**
     * 构建业务数据。
     *
     * @param modelConfig 模型配置。
     * @param enableTools 是否启用工具。
     * @param extraAdvisors 额外 AgentEnhancer 列表。
     * @return 处理后的结果
     */
    @Override
    public ChatClient buildChatClient(ModelConfig modelConfig, boolean enableTools, CallAdvisor... extraAdvisors) {
        return armoryStrategyFactory.chatClient(modelConfig, enableTools, extraAdvisors);
    }
}
