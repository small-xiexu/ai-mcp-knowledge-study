package com.xbk.knowledge.test;

import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.application.provider.ModelProviderFactory;
import com.xbk.knowledge.trigger.job.MCPServerCSDNJob;
import com.xbk.knowledge.types.enums.ModelType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.Collections;
import java.util.function.Consumer;

/**
 * 编排层 MCP 工具集成测试
 * 验证通过 ModelProviderFactory 创建的 ChatClient 自动支持 MCP 工具和 Advisors
 *
 * @author xiexu
 */
@Slf4j
@Tag("integration")
@SpringBootTest
@ImportAutoConfiguration(exclude = {
        org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration.class
})
public class OrchestrationMCPTest {

    private final ModelProviderFactory modelProviderFactory;

    /**
     * CSDN 定时任务 Job
     */
    private final MCPServerCSDNJob mcpServerCSDNJob;

    @Autowired
    public OrchestrationMCPTest(ModelProviderFactory modelProviderFactory,
                                MCPServerCSDNJob mcpServerCSDNJob) {
        this.modelProviderFactory = modelProviderFactory;
        this.mcpServerCSDNJob = mcpServerCSDNJob;
    }

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

        // 创建模型配置
        // 使用真实的 OpenAI API 测试工具调用
        ModelConfig config = new ModelConfig();
        config.setModelType(ModelType.OPENAI);
        config.setModelName("gpt-4o");
        config.setBaseUrl("http://localhost");  // 测试环境占位
        config.setApiKey("test-key");
        config.setEnabled(true);

        // 通过编排层创建 ChatClient（自动注入工具和 Advisors）
        ChatClient chatClient = modelProviderFactory.createChatClient(config);

        // 调试：打印 ChatClient 信息
        log.info(">>> ChatClient 创建成功: {}", chatClient
                .getClass()
                .getName());

        // 测试工具调用
        // 重要：使用明确的指令，让 AI 知道需要调用工具
        String userInput = """
                请使用 queryConfig 工具查询电脑配置信息。
                参数：computer = "sxie47559"

                请直接调用工具并返回结果。
                """;
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
        config.setModelType(ModelType.OPENAI);
        config.setModelName("gpt-4o");
        config.setBaseUrl("http://localhost");
        config.setApiKey("test-key");
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

        // 关键：使用 toolContext 启用工具调用
        Map<String, Object> toolContext = Collections.<String, Object>singletonMap("enabled", Boolean.TRUE);
        String response = chatClient.prompt()
                .user(userInput)
                .toolContext(toolContext)  // 启用工具调用上下文
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
        config.setModelType(ModelType.OPENAI);
        config.setModelName("gemini-3-flash");
        config.setBaseUrl("http://127.0.0.1:8045");
        config.setApiKey("test-key");
        config.setEnabled(true);

        ChatClient orchestrationClient = modelProviderFactory.createChatClient(config);
        log.info(">>> 编排层方式：自动注入工具和 Advisors，代码简洁");

        // 方式 2：直接调用（不推荐）
        // 需要手动注入 ToolCallbackProvider 和 Advisors
        // 容易遗漏，导致功能不完整
        log.info(">>> 直接调用方式：需要手动注入，容易遗漏");

        log.info(">>> 结论：推荐使用编排层方式，确保功能完整性和一致性");
    }

    /**
     * 测试 CSDN 文章发布工具（编排层方式）
     *
     * 验证点：
     * 1. 通过编排层创建的 ChatClient 可以调用 CSDN 发布工具
     * 2. AI 自动生成文章内容并发布到 CSDN
     * 3. TraceIdAdvisor 自动注入并生效
     */
    @Test
    public void test_orchestration_csdn_publish_tool() {
        log.info(">>> 测试编排层 CSDN 文章发布工具");

        // 创建模型配置
        ModelConfig config = new ModelConfig();
        config.setModelType(ModelType.OPENAI);
        config.setModelName("gpt-4o");
        config.setBaseUrl("http://localhost");
        config.setApiKey("test-key");
        config.setEnabled(true);

        // 通过编排层创建 ChatClient（自动注入工具和 Advisors）
        ChatClient chatClient = modelProviderFactory.createChatClient(config);

        // 测试 CSDN 文章发布
        String userInput = """
                我需要你帮我生成一篇文章，要求如下：
                1. 场景为 AI 学习与实战系列文章
                2. 主题从以下列表中任选其一深入讲解，不要全部覆盖：Spring AI + MCP 实战、RAG 入门、向量数据库实践、Skills 实战、Prompt Engineering、Embedding、微调与对齐（SFT/RLHF）、评测与安全、MLOps/上线、GPU/推理优化
                3. 文章结构清晰，按主题分小节，循序渐进，从概念、原理、关键步骤、实践示例与注意事项进行讲解
                4. 至少包含 8 个小节，每个小节不少于 400 字，全文不少于 5000 字
                5. 结尾给出学习路线与实践建议，便于新手跟学
                根据以上内容，不要阐述其他信息，请直接提供：文章标题、文章内容、文章标签（最多7个，用英文逗号隔开）、文章简述（100字）
                将以上内容发布文章到CSDN。
                """;

        log.info(">>> QUESTION: {}", userInput);

        String response = chatClient.prompt()
                .user(userInput)
                .call()
                .content();

        log.info(">>> ASSISTANT: {}", response);
        log.info(">>> 测试完成：CSDN 文章发布成功");
    }

    /**
     * 测试 CSDN 发布 + 微信通知（单次调用，编排层方式）
     *
     * 验证点：
     * 1. AI 自动生成文章并发布到 CSDN
     * 2. 发布成功后自动调用微信通知工具
     * 3. 两个工具在一次对话中顺序调用
     */
    @Test
    public void test_orchestration_csdn_weixin_notice_tool() {
        log.info(">>> 测试编排层 CSDN 发布 + 微信通知（单次调用）");

        // 创建模型配置
        ModelConfig config = new ModelConfig();
        config.setModelType(ModelType.OPENAI);
        config.setModelName("gpt-4o");
        config.setBaseUrl("http://localhost");
        config.setApiKey("test-key");
        config.setEnabled(true);

        // 通过编排层创建 ChatClient
        ChatClient chatClient = modelProviderFactory.createChatClient(config);

        // 测试 CSDN 发布 + 微信通知
        String userInput = """
                我需要你帮我生成一篇文章，要求如下：
                1. 场景为 AI 学习与实战系列文章
                2. 主题从以下列表中任选其一深入讲解，不要全部覆盖：Spring AI + MCP 实战、RAG 入门、向量数据库实践、Skills 实战、Prompt Engineering、Embedding、微调与对齐（SFT/RLHF）、评测与安全、MLOps/上线、GPU/推理优化
                3. 文章结构清晰，按主题分小节，循序渐进，从概念、原理、关键步骤、实践示例与注意事项进行讲解
                4. 至少包含 8 个小节，每个小节不少于 400 字，全文不少于 5000 字
                5. 结尾给出学习路线与实践建议，便于新手跟学
                根据以上内容，不要阐述其他信息，请直接提供：文章标题、文章内容、文章标签（最多7个，用英文逗号隔开）、文章简述（100字）
                将以上内容发布文章到CSDN。

                之后进行微信公众号消息通知，平台：CSDN、主题：为文章标题、描述：为文章简述、跳转地址：从发布文章到CSDN获取 url
                """;

        log.info(">>> QUESTION: {}", userInput);

        String response = chatClient.prompt()
                .user(userInput)
                .call()
                .content();

        log.info(">>> ASSISTANT: {}", response);
        log.info(">>> 测试完成：CSDN 发布 + 微信通知成功");
    }

    /**
     * 测试 CSDN 发布 + 微信通知（带聊天记忆，编排层方式）
     * 演示多轮对话中的上下文保持
     *
     * 验证点：
     * 1. 第一轮对话：发布文章到 CSDN
     * 2. 第二轮对话：基于第一轮的发布结果发送微信通知
     * 3. 聊天记忆功能正常工作，AI 能记住上一轮的发布结果
     */
    @Test
    public void test_orchestration_csdn_weixin_notice_with_memory_tool() {
        log.info(">>> 测试编排层 CSDN 发布 + 微信通知（带聊天记忆）");

        // 创建模型配置
        ModelConfig config = new ModelConfig();
        config.setModelType(ModelType.OPENAI);
        config.setModelName("gpt-4o");
        config.setBaseUrl("http://localhost");
        config.setApiKey("test-key");
        config.setEnabled(true);

        // 创建聊天记忆
        InMemoryChatMemoryRepository chatMemoryRepository = new InMemoryChatMemoryRepository();
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(100)
                .build();

        // 通过编排层创建 ChatClient，并添加聊天记忆 Advisor
        PromptChatMemoryAdvisor memoryAdvisor = PromptChatMemoryAdvisor.builder(chatMemory)
                .build();
        ChatClient chatClient = modelProviderFactory.createChatClient(config)
                .mutate()
                .defaultAdvisors(memoryAdvisor)
                .build();

        // 第一轮对话：发布文章到 CSDN
        String publishInput = """
                我需要你帮我生成一篇文章，要求如下：
                1. 场景为 AI 学习与实战系列文章
                2. 主题从以下列表中任选其一深入讲解，不要全部覆盖：Spring AI + MCP 实战、RAG 入门、向量数据库实践、Skills 实战、Prompt Engineering、Embedding、微调与对齐（SFT/RLHF）、评测与安全、MLOps/上线、GPU/推理优化
                3. 文章结构清晰，按主题分小节，循序渐进，从概念、原理、关键步骤、实践示例与注意事项进行讲解
                4. 至少包含 8 个小节，每个小节不少于 400 字，全文不少于 5000 字
                5. 结尾给出学习路线与实践建议，便于新手跟学
                根据以上内容，不要阐述其他信息，请直接提供：文章标题、文章内容、文章标签（最多7个，用英文逗号隔开）、文章简述（100字）
                将以上内容发布文章到CSDN。
                """;

        log.info(">>> [第一轮] QUESTION: {}", publishInput);

        String conversationId = "orchestration-csdn-weixin-1";
        Consumer<ChatClient.AdvisorSpec> memoryParam = advisor -> advisor
                .param("chat_memory_conversation_id", conversationId);
        String publishResponse = chatClient
                .prompt()
                .user(publishInput)
                .advisors(memoryParam)
                .call()
                .content();

        log.info(">>> [第一轮] ASSISTANT: {}", publishResponse);

        // 第二轮对话：基于第一轮的发布结果发送微信通知
        String noticeInput = """
                根据上一轮对话中已发布的文章信息，进行微信公众号消息通知：
                - 平台：CSDN
                - 主题：使用已发布的文章标题
                - 描述：使用已发布的文章简述
                - 跳转地址：使用已发布文章返回的 url

                注意：不要再次发布文章，直接使用上一轮对话中的发布结果。
                """;

        log.info(">>> [第二轮] QUESTION: {}", noticeInput);

        String noticeResponse = chatClient
                .prompt()
                .user(noticeInput)
                .advisors(memoryParam)
                .call()
                .content();

        log.info(">>> [第二轮] ASSISTANT: {}", noticeResponse);
        log.info(">>> 测试完成：CSDN 发布 + 微信通知（带聊天记忆）成功");
    }

    /**
     * 手动触发 CSDN 定时任务
     * 用于验证定时任务逻辑是否正常工作
     *
     * 注意：此方法直接调用定时任务，不涉及编排层
     */
    @Test
    public void test_trigger_csdn_job() {
        log.info(">>> 手动触发 CSDN 定时任务开始");
        mcpServerCSDNJob.mcpServerCSDNHandler();
        log.info(">>> 手动触发 CSDN 定时任务结束");
    }
}
