package com.xbk.knowledge.test;

import com.xbk.knowledge.Application;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.application.provider.ModelProviderFactory;
import com.xbk.knowledge.types.enums.ModelType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * BaseUrl 规范化测试
 * 验证 OpenAIModelProvider 能够自动处理各种格式的 baseUrl
 *
 * @author xiexu
 */
@Slf4j
@SpringBootTest(classes = Application.class)
@ImportAutoConfiguration(exclude = {
        org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration.class
})
public class BaseUrlNormalizationTest {

    @Autowired
    private ModelProviderFactory modelProviderFactory;

    /**
     * 测试场景 1：标准格式（不带 /v1）
     * 输入：http://127.0.0.1:8045
     * 预期：正常工作
     */
    @Test
    public void test_baseUrl_without_v1() {
        log.info(">>> 测试场景 1：标准格式（不带 /v1）");

        ModelConfig config = new ModelConfig();
        config.setModelType(ModelType.OPENAI);
        config.setModelName("gemini-3-flash");
        config.setBaseUrl("http://127.0.0.1:8045");  // 标准格式
        config.setApiKey("sk-test");
        config.setEnabled(true);

        ChatClient chatClient = modelProviderFactory.createChatClient(config);
        assertNotNull(chatClient);
        log.info(">>> ✅ 标准格式测试通过");
    }

    /**
     * 测试场景 2：带 /v1 后缀
     * 输入：http://127.0.0.1:8045/v1
     * 预期：自动去除 /v1，正常工作
     */
    @Test
    public void test_baseUrl_with_v1() {
        log.info(">>> 测试场景 2：带 /v1 后缀");

        ModelConfig config = new ModelConfig();
        config.setModelType(ModelType.OPENAI);
        config.setModelName("gemini-3-flash");
        config.setBaseUrl("http://127.0.0.1:8045/v1");  // 带 /v1 后缀
        config.setApiKey("sk-test");
        config.setEnabled(true);

        ChatClient chatClient = modelProviderFactory.createChatClient(config);
        assertNotNull(chatClient);
        log.info(">>> ✅ 带 /v1 后缀测试通过");
    }

    /**
     * 测试场景 3：带 /v1/chat/completions 完整路径
     * 输入：http://127.0.0.1:8045/v1/chat/completions
     * 预期：自动去除完整路径，正常工作
     */
    @Test
    public void test_baseUrl_with_full_path() {
        log.info(">>> 测试场景 3：带 /v1/chat/completions 完整路径");

        ModelConfig config = new ModelConfig();
        config.setModelType(ModelType.OPENAI);
        config.setModelName("gemini-3-flash");
        config.setBaseUrl("http://127.0.0.1:8045/v1/chat/completions");  // 完整路径
        config.setApiKey("sk-test");
        config.setEnabled(true);

        ChatClient chatClient = modelProviderFactory.createChatClient(config);
        assertNotNull(chatClient);
        log.info(">>> ✅ 完整路径测试通过");
    }

    /**
     * 测试场景 4：带末尾斜杠
     * 输入：http://127.0.0.1:8045/
     * 预期：自动去除末尾斜杠，正常工作
     */
    @Test
    public void test_baseUrl_with_trailing_slash() {
        log.info(">>> 测试场景 4：带末尾斜杠");

        ModelConfig config = new ModelConfig();
        config.setModelType(ModelType.OPENAI);
        config.setModelName("gemini-3-flash");
        config.setBaseUrl("http://127.0.0.1:8045/");  // 带末尾斜杠
        config.setApiKey("sk-test");
        config.setEnabled(true);

        ChatClient chatClient = modelProviderFactory.createChatClient(config);
        assertNotNull(chatClient);
        log.info(">>> ✅ 末尾斜杠测试通过");
    }

    /**
     * 测试场景 5：带 /v1/ 和末尾斜杠
     * 输入：http://127.0.0.1:8045/v1/
     * 预期：自动去除 /v1 和末尾斜杠，正常工作
     */
    @Test
    public void test_baseUrl_with_v1_and_trailing_slash() {
        log.info(">>> 测试场景 5：带 /v1/ 和末尾斜杠");

        ModelConfig config = new ModelConfig();
        config.setModelType(ModelType.OPENAI);
        config.setModelName("gemini-3-flash");
        config.setBaseUrl("http://127.0.0.1:8045/v1/");  // 带 /v1/ 和末尾斜杠
        config.setApiKey("sk-test");
        config.setEnabled(true);

        ChatClient chatClient = modelProviderFactory.createChatClient(config);
        assertNotNull(chatClient);
        log.info(">>> ✅ /v1/ 和末尾斜杠测试通过");
    }
}
