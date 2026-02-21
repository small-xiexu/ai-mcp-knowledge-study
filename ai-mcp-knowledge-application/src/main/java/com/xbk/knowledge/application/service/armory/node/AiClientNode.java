package com.xbk.knowledge.application.service.armory.node;

import com.xbk.knowledge.application.service.armory.factory.AiClientArmoryContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 客户端节点，负责构建增强后的 ChatClient。
 * @author sxie
 */
@Component
public class AiClientNode extends AbstractAiClientArmoryNode {

    @Override
    protected void doHandle(AiClientArmoryContext context) {
        ChatModel chatModel = context.getChatModel();
        if (chatModel == null) {
            throw new IllegalStateException("ChatModel 尚未构建，无法生成 ChatClient");
        }
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        if (context.isResolvedEnableTools() && context.getToolCallbackProvider() != null) {
            builder.defaultToolCallbacks(context.getToolCallbackProvider());
        }
        List<CallAdvisor> mergedAdvisors = context.getMergedAdvisors();
        if (mergedAdvisors != null && !mergedAdvisors.isEmpty()) {
            builder.defaultAdvisors(mergedAdvisors.toArray(new CallAdvisor[0]));
        }
        ChatClient chatClient = builder.build();
        context.setChatClient(chatClient);
    }
}
