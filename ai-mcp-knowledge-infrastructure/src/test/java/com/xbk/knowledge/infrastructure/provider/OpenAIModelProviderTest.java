package com.xbk.knowledge.infrastructure.provider;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证 OpenAI baseUrl 规范化逻辑，防止路径重复导致调用失败。
 *
 * @author xiexu
 */
public class OpenAIModelProviderTest {

    /**
     * 对外暴露 shouldNormalizeBaseUrl 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldNormalizeBaseUrl() throws Exception {
        OpenAIModelProvider provider = new OpenAIModelProvider();
        Method method = OpenAIModelProvider.class.getDeclaredMethod("normalizeBaseUrl", String.class);
        method.setAccessible(true);

        String trimmed = (String) method.invoke(provider, "http://localhost/v1/chat/completions");
        String withoutV1 = (String) method.invoke(provider, "http://localhost/v1");
        String withoutSlash = (String) method.invoke(provider, "http://localhost/");

        assertEquals("http://localhost", trimmed);
        assertEquals("http://localhost", withoutV1);
        assertEquals("http://localhost", withoutSlash);
    }
}
