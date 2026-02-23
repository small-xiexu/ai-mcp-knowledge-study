package com.xbk.knowledge.test;

import com.xbk.knowledge.config.trace.TraceIdAgentEnhancer;
import com.xbk.knowledge.trigger.job.MCPServerCSDNJob;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.function.Consumer;

/**
 * MCP 工具调用测试类（遗留代码 - 仅供参考）
 * 演示使用不同大模型进行 Function Calling
 *
 * ⚠️ 警告：本测试类使用的是过时的实现方式！
 *
 * 问题：
 * 1. 直接使用底层 ChatModel，手动注入工具和 AgentEnhancers
 * 2. 绕过了编排层，无法享受以下能力：
 * - 模型自动选择（基于运行策略）
 * - 模型降级和故障转移
 * - 自动重试机制
 * - 统一的监控和日志
 * 3. 依赖已删除的配置类（OpenAIConfig、GeminiConfig 等）
 * 4. 代码重复，每个测试都要手动构建 ChatClient
 *
 * ✅ 推荐方式：使用编排层
 * - 测试类：OrchestrationMCPTest.java
 * - 核心类：ModelProviderFactory（ai-mcp-knowledge-application 模块）
 * - 优势：自动注入 MCP 工具和 AgentEnhancers，享受完整的编排能力
 *
 * 📝 保留原因：
 * - 作为"如何不应该做"的反面教材
 * - 展示底层 Spring AI API 的使用方式
 * - 帮助理解编排层的价值
 *
 * @author xiexu
 * @deprecated 请使用 OrchestrationMCPTest.java 代替
 */
@Deprecated
@Slf4j
@Tag("integration")
@SpringBootTest
@EnableAutoConfiguration(exclude = {
        org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration.class
})
public class MCPTest {

    private final ToolCallbackProvider tools;

    /**
     * Gemini 模型（通过 OpenAI 兼容协议调用）
     */
    private final OpenAiChatModel openAiChatModel;

    /**
     * CSDN 定时任务 Job
     */
    private final MCPServerCSDNJob mcpServerCSDNJob;

    /**
     * TraceId 链路追踪 AgentEnhancer
     */
    private final TraceIdAgentEnhancer traceIdAdvisor;

    @Autowired
    public MCPTest(ToolCallbackProvider tools,
                   OpenAiChatModel openAiChatModel,
                   MCPServerCSDNJob mcpServerCSDNJob,
                   TraceIdAgentEnhancer traceIdAdvisor) {
        this.tools = tools;
        this.openAiChatModel = openAiChatModel;
        this.mcpServerCSDNJob = mcpServerCSDNJob;
        this.traceIdAdvisor = traceIdAdvisor;
    }

    /**
     * traceId 传递指令模板
     * 通过 System Prompt 告知 AI 在调用工具时传递 traceId 参数
     */
    private static final String TRACE_ID_SYSTEM_PROMPT = """
            [链路追踪指令]
            当前请求的 traceId 为: %s
            在调用任何工具时，请将此 traceId 作为参数传递（参数名: traceId）。
            这用于端到端日志关联，请务必传递。
            """;

    /**
     * 测试 Gemini 模型的工具调用能力
     * 使用 Google AI Gemini 2.0 Flash 模型
     *
     * @deprecated 请使用 OrchestrationMCPTest.test_orchestration_with_mcp_tools() 代替
     */
    @Test
    @Deprecated
    public void test_gemini_tool() {
        String userInput = "有哪些工具可以使用";
        String traceId = TraceIdAgentEnhancer.getCurrentTraceId();

        // 使用 Gemini 模型创建 ChatClient，注入 TraceIdAgentEnhancer
        ChatClient chatClient = ChatClient.builder(openAiChatModel)
                .defaultToolCallbacks(tools)
                .defaultAdvisors(traceIdAdvisor)
                .build();

        log.info(">>> QUESTION: {}", userInput);
        String systemPrompt = String
                .format(TRACE_ID_SYSTEM_PROMPT, traceId);
        String assistantContent = chatClient
                .prompt()
                .system(systemPrompt)
                .user(userInput)
                .call()
                .content();
        log.info(">>> ASSISTANT: {}", assistantContent);
    }

    /**
     * 测试微信通知工具
     *
     * @deprecated 请使用 OrchestrationMCPTest.test_orchestration_weixin_notice_tool() 代替
     */
    @Test
    @Deprecated
    public void test_weixin_notice_tool() {
        String userInput = """
                请调用工具发送微信公众号模板消息，参数如下：
                platform=AI-MCP-Study
                subject=测试通知
                description=这是一条用于验证 MCP 工具调用的测试消息
                jumpUrl=https://example.com/mcp-test
                """;
        String traceId = TraceIdAgentEnhancer.getCurrentTraceId();

        ChatClient chatClient = ChatClient.builder(openAiChatModel)
                .defaultToolCallbacks(tools)
                .defaultAdvisors(traceIdAdvisor)
                .build();

        log.info(">>> QUESTION: {}", userInput);
        String systemPrompt = String
                .format(TRACE_ID_SYSTEM_PROMPT, traceId);
        String assistantContent = chatClient
                .prompt()
                .system(systemPrompt)
                .user(userInput)
                .call()
                .content();
        log.info(">>> ASSISTANT: {}", assistantContent);
    }

    /**
     * 测试 CSDN 文章发布工具
     *
     * @deprecated 此测试方法使用过时的实现方式，仅供参考
     */
    @Test
    @Deprecated
    public void test_csdn_publish_tool() {
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
        String traceId = TraceIdAgentEnhancer.getCurrentTraceId();

        ChatClient chatClient = ChatClient.builder(openAiChatModel)
                .defaultToolCallbacks(tools)
                .defaultAdvisors(traceIdAdvisor)
                .build();

        log.info("\n>>> QUESTION: {}", userInput);
        String systemPrompt = String
                .format(TRACE_ID_SYSTEM_PROMPT, traceId);
        String assistantContent = chatClient
                .prompt()
                .system(systemPrompt)
                .user(userInput)
                .call()
                .content();
        log.info("\n>>> ASSISTANT: {}", assistantContent);
    }

    /**
     * 测试 CSDN 发布 + 微信通知（单次调用）
     *
     * @deprecated 此测试方法使用过时的实现方式，仅供参考
     */
    @Test
    @Deprecated
    public void test_csdn_weixin_notice_tool() {
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
        String traceId = TraceIdAgentEnhancer.getCurrentTraceId();

        ChatClient chatClient = ChatClient.builder(openAiChatModel)
                .defaultToolCallbacks(tools)
                .defaultAdvisors(traceIdAdvisor)
                .build();

        log.info("\n>>> QUESTION: {}", userInput);
        String systemPrompt = String
                .format(TRACE_ID_SYSTEM_PROMPT, traceId);
        String assistantContent = chatClient
                .prompt()
                .system(systemPrompt)
                .user(userInput)
                .call()
                .content();
        log.info("\n>>> ASSISTANT: {}", assistantContent);
    }

    /**
     * 测试 CSDN 发布 + 微信通知（带聊天记忆）
     * 演示多轮对话中的上下文保持
     *
     * @deprecated 此测试方法使用过时的实现方式，仅供参考
     */
    @Test
    @Deprecated
    public void test_csdn_weixin_notice_with_memory_tool() {
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
        String traceId = TraceIdAgentEnhancer.getCurrentTraceId();

        InMemoryChatMemoryRepository chatMemoryRepository = new InMemoryChatMemoryRepository();
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(100)
                .build();
        PromptChatMemoryAdvisor memoryAdvisor = PromptChatMemoryAdvisor.builder(chatMemory)
                .build();
        ChatClient chatClient = ChatClient.builder(openAiChatModel)
                .defaultToolCallbacks(tools)
                .defaultAdvisors(traceIdAdvisor, memoryAdvisor)
                .build();

        log.info("\n>>> QUESTION: {}", userInput);
        String systemPrompt = String
                .format(TRACE_ID_SYSTEM_PROMPT, traceId);
        String conversationId = "mcp-csdn-weixin-1";
        Consumer<ChatClient.AdvisorSpec> conversationAdvisor = advisor -> advisor
                .param("chat_memory_conversation_id", conversationId);
        String assistantContent = chatClient
                .prompt()
                .system(systemPrompt)
                .user(userInput)
                .advisors(conversationAdvisor)
                .call()
                .content();
        log.info("\n>>> ASSISTANT: {}", assistantContent);

        String noticeInput = """
                根据上一轮对话中已发布的文章信息，进行微信公众号消息通知：
                - 平台：CSDN
                - 主题：使用已发布的文章标题
                - 描述：使用已发布的文章简述
                - 跳转地址：使用已发布文章返回的 url

                注意：不要再次发布文章，直接使用上一轮对话中的发布结果。
                """;
        log.info("\n>>> QUESTION: {}", noticeInput);
        String noticeContent = chatClient
                .prompt()
                .system(systemPrompt)
                .user(noticeInput)
                .advisors(conversationAdvisor)
                .call()
                .content();
        log.info("\n>>> ASSISTANT: {}", noticeContent);
    }

    /**
     * 手动触发 CSDN 定时任务
     * 用于验证定时任务逻辑是否正常工作
     *
     * 注意：此方法仍然有效，不是过时的实现
     */
    @Test
    public void test_trigger_csdn_job() {
        log.info(">>> 手动触发 CSDN 定时任务开始");
        mcpServerCSDNJob.mcpServerCSDNHandler();
        log.info(">>> 手动触发 CSDN 定时任务结束");
    }

}
