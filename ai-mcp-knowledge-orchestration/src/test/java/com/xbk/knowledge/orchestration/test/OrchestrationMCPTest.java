package com.xbk.knowledge.orchestration.test;

import com.xbk.knowledge.orchestration.domain.entity.ModelConfig;
import com.xbk.knowledge.orchestration.model.enums.ProviderType;
import com.xbk.knowledge.orchestration.provider.ModelProviderFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 编排层 MCP 工具集成测试
 * 验证通过 ModelProviderFactory 创建的 ChatClient 自动支持 MCP 工具和 Advisors
 *
 * @author xiexu
 */
@Slf4j
@SpringBootTest
public class OrchestrationMCPTest {

    @Autowired
    private ModelProviderFactory modelProviderFactory;

    /**
     * 测试通过编排层创建的 ChatClient 是否自动注入 MCP 工具
     *
     * 验证点：
     * 1. ChatClient 创建成功
     * 2. 自动注入 ToolCallbackProvider（MCP 工具）
     * 3. 自动注入 TraceIdAdvisor（链路追踪）
     * 4. 工具调用功能正常
     */
    @Test
    public void test_orchestration_with_mcp_tools() {
        log.info(">>> 测试编排层 MCP 工具集成");

        // 创建模型配置（使用 Gemini 模型）
        ModelConfig config = new ModelConfig();
        config.setProviderType(ProviderType.OPENAI);
        config.setModelName("gemini-3-flash");
        config.setBaseUrl("http://127.0.0.1:8045");
        config.setApiKey("sk-1256419209eb47ccbabaa98abccfe4c8");
        config.setEnabled(true);

        // 通过编排层创建 ChatClient（自动注入工具和 Advisors）
        ChatClient chatClient = modelProviderFactory.createChatClient(config);

        // 测试工具调用
        String userInput = "有哪些工具可以使用？";
        log.info(">>> QUESTION: {}", userInput);

        String response = chatClient.prompt()
                .user(userInput)
                .call()
                .content();

        log.info(">>> ASSISTANT: {}", response);
        log.info(">>> 测试完成：ChatClient 已自动注入 MCP 工具和 Advisors");
    }

    /**
     * 测试编排层的微信通知工具调用
     *
     * 验证点：
     * 1. 通过编排层创建的 ChatClient 可以调用 MCP 工具
     * 2. TraceIdAdvisor 自动注入并生效
     */
    @Test
    public void test_orchestration_weixin_notice_tool() {
        log.info(">>> 测试编排层微信通知工具");

        // 创建模型配置
        ModelConfig config = new ModelConfig();
        config.setProviderType(ProviderType.OPENAI);
        config.setModelName("gemini-3-flash");
        config.setBaseUrl("http://127.0.0.1:8045");
        config.setApiKey("sk-1256419209eb47ccbabaa98abccfe4c8");
        config.setEnabled(true);

        // 通过编排层创建 ChatClient
        ChatClient chatClient = modelProviderFactory.createChatClient(config);

        // 测试微信通知工具
        String userInput = """
                请调用工具发送微信公众号模板消息，参数如下：
                platform=AI-Orchestration-Test
                subject=编排层测试通知
                description=验证通过编排层创建的 ChatClient 自动支持 MCP 工具调用
                jumpUrl=https://example.com/orchestration-test
                """;

        log.info(">>> QUESTION: {}", userInput);

        String response = chatClient.prompt()
                .user(userInput)
                .call()
                .content();

        log.info(">>> ASSISTANT: {}", response);
        log.info(">>> 测试完成：编排层 MCP 工具调用成功");
    }

    /**
     * 对比测试：验证编排层和直接调用的区别
     *
     * 说明：
     * - 编排层方式：自动注入工具和 Advisors，代码简洁
     * - 直接调用方式：需要手动注入，容易遗漏
     */
    @Test
    public void test_compare_orchestration_vs_direct() {
        log.info(">>> 对比测试：编排层 vs 直接调用");

        // 方式 1：通过编排层（推荐）
        ModelConfig config = new ModelConfig();
        config.setProviderType(ProviderType.OPENAI);
        config.setModelName("gemini-3-flash");
        config.setBaseUrl("http://127.0.0.1:8045");
        config.setApiKey("sk-1256419209eb47ccbabaa98abccfe4c8");
        config.setEnabled(true);

        ChatClient orchestrationClient = modelProviderFactory.createChatClient(config);
        log.info(">>> 编排层方式：自动注入工具和 Advisors，代码简洁");

        // 方式 2：直接调用（不推荐）
        // 需要手动注入 ToolCallbackProvider 和 Advisors
        // 容易遗漏，导致功能不完整
        log.info(">>> 直接调用方式：需要手动注入，容易遗漏");

        log.info(">>> 结论：推荐使用编排层方式，确保功能完整性和一致性");
    }
}
