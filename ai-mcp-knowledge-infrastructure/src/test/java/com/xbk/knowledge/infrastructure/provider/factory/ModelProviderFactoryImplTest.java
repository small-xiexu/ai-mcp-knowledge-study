package com.xbk.knowledge.infrastructure.provider.factory;

import com.xbk.knowledge.application.provider.ModelProvider;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.types.enums.ModelType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
     * 对外暴露 shouldDelegateCreateChatModel 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldDelegateCreateChatModel() {
        ModelProvider openai = Mockito.mock(ModelProvider.class);
        when(openai.getModelType()).thenReturn(ModelType.OPENAI);
        ChatModel chatModel = Mockito.mock(ChatModel.class);
        when(openai.createChatModel(Mockito.any(ModelConfig.class))).thenReturn(chatModel);

        ModelProviderFactoryImpl factory = new ModelProviderFactoryImpl(Collections.<ModelProvider>singletonList(openai));

        ChatModel created = factory.getProvider(ModelType.OPENAI)
                .createChatModel(ModelConfig.builder().modelType(ModelType.OPENAI).build());

        assertEquals(chatModel, created);
    }
}
