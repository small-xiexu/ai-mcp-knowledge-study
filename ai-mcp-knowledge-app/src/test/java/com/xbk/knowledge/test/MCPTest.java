package com.xbk.knowledge.test;

import com.xbk.knowledge.config.TraceIdAdvisor;
import com.xbk.knowledge.trigger.job.MCPServerCSDNJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * MCP 工具调用测试类
 * 演示使用不同大模型进行 Function Calling
 *
 * @author xiexu
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class MCPTest {

    @Resource
    private ChatClient.Builder chatClientBuilder;

    @Autowired
    private ToolCallbackProvider tools;

    /**
     * Gemini 模型（通过 OpenAI 兼容协议调用）
     */
    @Autowired
    private OpenAiChatModel openAiChatModel;

    /**
     * CSDN 定时任务 Job
     */
    @Autowired
    private MCPServerCSDNJob mcpServerCSDNJob;

    /**
     * TraceId 链路追踪 Advisor
     */
    @Autowired
    private TraceIdAdvisor traceIdAdvisor;

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
     */
    @Test
    public void test_gemini_tool() {
        String userInput = "有哪些工具可以使用";
        String traceId = TraceIdAdvisor.getCurrentTraceId();

        // 使用 Gemini 模型创建 ChatClient，注入 TraceIdAdvisor
        var chatClient = ChatClient.builder(openAiChatModel)
                .defaultToolCallbacks(tools)
                .defaultAdvisors(traceIdAdvisor)
                .build();

        System.out.println("\n>>> QUESTION: " + userInput);
        System.out.println("\n>>> ASSISTANT: " + chatClient.prompt()
                .system(String.format(TRACE_ID_SYSTEM_PROMPT, traceId))
                .user(userInput)
                .call()
                .content());
    }

    @Test
    public void test_weixin_notice_tool() {
        String userInput = """
                请调用工具发送微信公众号模板消息，参数如下：
                platform=AI-MCP-Study
                subject=测试通知
                description=这是一条用于验证 MCP 工具调用的测试消息
                jumpUrl=https://example.com/mcp-test
                """;
        String traceId = TraceIdAdvisor.getCurrentTraceId();

        var chatClient = ChatClient.builder(openAiChatModel)
                .defaultToolCallbacks(tools)
                .defaultAdvisors(traceIdAdvisor)
                .build();

        System.out.println("\n>>> QUESTION: " + userInput);
        System.out.println("\n>>> ASSISTANT: " + chatClient.prompt()
                .system(String.format(TRACE_ID_SYSTEM_PROMPT, traceId))
                .user(userInput)
                .call()
                .content());
    }

    @Test
    public void test_csdn_publish_tool() {
        var userInput = """
                我需要你帮我生成一篇文章，要求如下：
                1. 场景为 AI 学习与实战系列文章
                2. 主题从以下列表中任选其一深入讲解，不要全部覆盖：Spring AI + MCP 实战、RAG 入门、向量数据库实践、Skills 实战、Prompt Engineering、Embedding、微调与对齐（SFT/RLHF）、评测与安全、MLOps/上线、GPU/推理优化
                3. 文章结构清晰，按主题分小节，循序渐进，从概念、原理、关键步骤、实践示例与注意事项进行讲解
                4. 至少包含 8 个小节，每个小节不少于 400 字，全文不少于 5000 字
                5. 结尾给出学习路线与实践建议，便于新手跟学
                根据以上内容，不要阐述其他信息，请直接提供：文章标题、文章内容、文章标签（最多7个，用英文逗号隔开）、文章简述（100字）
                将以上内容发布文章到CSDN。
                """;
        String traceId = TraceIdAdvisor.getCurrentTraceId();

        var chatClient = ChatClient.builder(openAiChatModel)
                .defaultToolCallbacks(tools)
                .defaultAdvisors(traceIdAdvisor)
                .build();

        log.info("\n>>> QUESTION: {}", userInput);
        log.info("\n>>> ASSISTANT: {}", chatClient.prompt()
                .system(String.format(TRACE_ID_SYSTEM_PROMPT, traceId))
                .user(userInput)
                .call()
                .content());
    }

    @Test
    public void test_csdn_weixin_notice_tool() {
        var userInput = """
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
        String traceId = TraceIdAdvisor.getCurrentTraceId();

        var chatClient = ChatClient.builder(openAiChatModel)
                .defaultToolCallbacks(tools)
                .defaultAdvisors(traceIdAdvisor)
                .build();

        log.info("\n>>> QUESTION: {}", userInput);
        log.info("\n>>> ASSISTANT: {}", chatClient.prompt()
                .system(String.format(TRACE_ID_SYSTEM_PROMPT, traceId))
                .user(userInput)
                .call()
                .content());
    }

    @Test
    public void test_csdn_weixin_notice_with_memory_tool() {
        var userInput = """
                我需要你帮我生成一篇文章，要求如下：
                1. 场景为 AI 学习与实战系列文章
                2. 主题从以下列表中任选其一深入讲解，不要全部覆盖：Spring AI + MCP 实战、RAG 入门、向量数据库实践、Skills 实战、Prompt Engineering、Embedding、微调与对齐（SFT/RLHF）、评测与安全、MLOps/上线、GPU/推理优化
                3. 文章结构清晰，按主题分小节，循序渐进，从概念、原理、关键步骤、实践示例与注意事项进行讲解
                4. 至少包含 8 个小节，每个小节不少于 400 字，全文不少于 5000 字
                5. 结尾给出学习路线与实践建议，便于新手跟学
                根据以上内容，不要阐述其他信息，请直接提供：文章标题、文章内容、文章标签（最多7个，用英文逗号隔开）、文章简述（100字）
                将以上内容发布文章到CSDN。
                """;
        String traceId = TraceIdAdvisor.getCurrentTraceId();

        var chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(100)
                .build();
        var chatClient = ChatClient.builder(openAiChatModel)
                .defaultToolCallbacks(tools)
                .defaultAdvisors(traceIdAdvisor, PromptChatMemoryAdvisor.builder(chatMemory).build())
                .build();

        log.info("\n>>> QUESTION: {}", userInput);
        log.info("\n>>> ASSISTANT: {}", chatClient
                .prompt()
                .system(String.format(TRACE_ID_SYSTEM_PROMPT, traceId))
                .user(userInput)
                .advisors(advisor -> advisor
                        .param("chat_memory_conversation_id", "mcp-csdn-weixin-1"))
                .call()
                .content());

        var noticeInput = """
                根据上一轮对话中已发布的文章信息，进行微信公众号消息通知：
                - 平台：CSDN
                - 主题：使用已发布的文章标题
                - 描述：使用已发布的文章简述
                - 跳转地址：使用已发布文章返回的 url

                注意：不要再次发布文章，直接使用上一轮对话中的发布结果。
                """;
        log.info("\n>>> QUESTION: {}", noticeInput);
        log.info("\n>>> ASSISTANT: {}", chatClient
                .prompt()
                .system(String.format(TRACE_ID_SYSTEM_PROMPT, traceId))
                .user(noticeInput)
                .advisors(advisor -> advisor
                        .param("chat_memory_conversation_id", "mcp-csdn-weixin-1"))
                .call()
                .content());
    }

    /**
     * 手动触发 CSDN 定时任务
     * 用于验证定时任务逻辑是否正常工作
     */
    @Test
    public void test_trigger_csdn_job() {
        log.info(">>> 手动触发 CSDN 定时任务开始");
        mcpServerCSDNJob.exec();
        log.info(">>> 手动触发 CSDN 定时任务结束");
    }

}
