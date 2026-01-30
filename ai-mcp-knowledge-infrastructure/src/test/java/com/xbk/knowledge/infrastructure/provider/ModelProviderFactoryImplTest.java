package com.xbk.knowledge.infrastructure.provider;

import com.xbk.knowledge.application.provider.ModelProvider;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.types.enums.ModelType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 验证模型提供者工厂的类型映射，避免模型类型错配。
 *
 * @author xiexu
 */
public class ModelProviderFactoryImplTest {

    /**
     * 对外暴露 shouldReturnProviderByType 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldReturnProviderByType() {
        ModelProvider openai = Mockito.mock(ModelProvider.class);
        when(openai.getModelType()).thenReturn(ModelType.OPENAI);
        ModelProvider gemini = Mockito.mock(ModelProvider.class);
        when(gemini.getModelType()).thenReturn(ModelType.GEMINI);

        ModelProviderFactoryImpl factory = new ModelProviderFactoryImpl(Arrays.asList(openai, gemini));

        assertEquals(openai, factory.getProvider(ModelType.OPENAI));
        assertTrue(factory.isSupported(ModelType.GEMINI));
    }

    /**
     * 对外暴露 shouldThrowForUnsupportedType 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldThrowForUnsupportedType() {
        ModelProvider openai = Mockito.mock(ModelProvider.class);
        when(openai.getModelType()).thenReturn(ModelType.OPENAI);
        ModelProviderFactoryImpl factory = new ModelProviderFactoryImpl(Collections.<ModelProvider>singletonList(openai));

        assertThrows(IllegalArgumentException.class, () -> factory.getProvider(ModelType.ANTHROPIC));
    }

    /**
     * 对外暴露 shouldDelegateCreateChatClient 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldDelegateCreateChatClient() {
        ModelProvider openai = Mockito.mock(ModelProvider.class);
        when(openai.getModelType()).thenReturn(ModelType.OPENAI);
        ChatClient client = Mockito.mock(ChatClient.class);
        when(openai.createChatClient(any(ModelConfig.class))).thenReturn(client);

        ModelProviderFactoryImpl factory = new ModelProviderFactoryImpl(Collections.<ModelProvider>singletonList(openai));

        ChatClient created = factory.createChatClient(ModelConfig.builder().modelType(ModelType.OPENAI).build());

        assertEquals(client, created);
    }
}
