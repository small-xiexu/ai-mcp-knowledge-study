package com.xbk.knowledge.application.service.armory.factory;

import com.xbk.knowledge.application.provider.ModelProvider;
import com.xbk.knowledge.application.provider.ModelProviderFactory;
import com.xbk.knowledge.application.service.armory.node.AiClientAdvisorNode;
import com.xbk.knowledge.application.service.armory.node.AiClientModelNode;
import com.xbk.knowledge.application.service.armory.node.AiClientNode;
import com.xbk.knowledge.application.service.armory.node.AiClientToolNode;
import com.xbk.knowledge.application.service.armory.node.RootNode;
import com.xbk.knowledge.config.ai.GlobalChatAdvisor;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.types.enums.ModelType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 客户端装配工厂单测。
 */
public class DefaultAiClientArmoryStrategyFactoryTest {

    @Test
    public void shouldAssembleChatClientByNodeChain() {
        ModelProviderFactory providerFactory = Mockito.mock(ModelProviderFactory.class);
        ModelProvider provider = Mockito.mock(ModelProvider.class);
        ChatModel chatModel = Mockito.mock(ChatModel.class);
        ToolCallbackProvider toolProvider = Mockito.mock(ToolCallbackProvider.class);
        ObjectProvider<ToolCallbackProvider> toolProviderObjectProvider = mockObjectProvider(toolProvider);
        GlobalAdvisor globalAdvisor = new GlobalAdvisor();

        ModelConfig modelConfig = ModelConfig.builder()
                .id(1L)
                .modelType(ModelType.OPENAI)
                .build();

        when(providerFactory.getProvider(ModelType.OPENAI)).thenReturn(provider);
        when(provider.createChatModel(modelConfig)).thenReturn(chatModel);

        RootNode rootNode = new RootNode();
        AiClientToolNode toolNode = new AiClientToolNode(toolProviderObjectProvider);
        AiClientAdvisorNode advisorNode = new AiClientAdvisorNode(List.of(globalAdvisor));
        AiClientModelNode modelNode = new AiClientModelNode(providerFactory);
        AiClientNode aiClientNode = new AiClientNode();
        DefaultAiClientArmoryStrategyFactory factory =
                new DefaultAiClientArmoryStrategyFactory(rootNode, toolNode, advisorNode, modelNode, aiClientNode);

        ChatClient assembled = factory.chatClient(modelConfig, true, new ExtraAdvisor());

        assertNotNull(assembled);
        verify(providerFactory).getProvider(ModelType.OPENAI);
        verify(provider).createChatModel(modelConfig);
    }

    @Test
    public void shouldRejectMissingModelType() {
        ModelProviderFactory providerFactory = Mockito.mock(ModelProviderFactory.class);
        ObjectProvider<ToolCallbackProvider> toolProviderObjectProvider = mockObjectProvider(null);
        RootNode rootNode = new RootNode();
        AiClientToolNode toolNode = new AiClientToolNode(toolProviderObjectProvider);
        AiClientAdvisorNode advisorNode = new AiClientAdvisorNode(List.of());
        AiClientModelNode modelNode = new AiClientModelNode(providerFactory);
        AiClientNode aiClientNode = new AiClientNode();
        DefaultAiClientArmoryStrategyFactory factory =
                new DefaultAiClientArmoryStrategyFactory(rootNode, toolNode, advisorNode, modelNode, aiClientNode);

        ModelConfig invalid = ModelConfig.builder().id(2L).build();
        assertThrows(IllegalArgumentException.class, () -> factory.chatClient(invalid, true));
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<ToolCallbackProvider> mockObjectProvider(ToolCallbackProvider provider) {
        ObjectProvider<ToolCallbackProvider> objectProvider = Mockito.mock(ObjectProvider.class);
        when(objectProvider.getIfAvailable()).thenReturn(provider);
        return objectProvider;
    }

    @GlobalChatAdvisor
    private static class GlobalAdvisor implements CallAdvisor {
        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            return chain.nextCall(request);
        }

        @Override
        public String getName() {
            return "global";
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }

    private static class ExtraAdvisor implements CallAdvisor {
        @Override
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            return chain.nextCall(request);
        }

        @Override
        public String getName() {
            return "extra";
        }

        @Override
        public int getOrder() {
            return Ordered.LOWEST_PRECEDENCE;
        }
    }
}
