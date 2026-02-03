package com.xbk.knowledge.trigger.job;

import com.xbk.knowledge.application.service.app.ChatClientAssemblyService;
import com.xbk.knowledge.types.trace.TraceIdUtils;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.stereotype.Service;

/**
 * CSDN 文章自动发布定时任务
 * <p>
 * 定时触发内容生产与分发，降低人工重复操作成本。
 * <p>
 * 工作流程：
 * <ol>
 *   <li>生成 AI 文章并发布到 CSDN</li>
 *   <li>获取发布结果（文章 URL）</li>
 *   <li>发送微信公众号通知</li>
 * </ol>
 * <p>
 * 技术要点：
 * <ul>
 *   <li>使用 ChatClient 与 Gemini 大模型交互</li>
 *   <li>通过 ToolCallbackProvider 注入 MCP 工具（CSDN 发布、微信通知）</li>
 *   <li>使用 PromptChatMemoryAdvisor 实现多轮对话记忆</li>
 *   <li>通过 System Prompt 传递 traceId 实现端到端链路追踪</li>
 * </ul>
 *
 * 职责：定时任务入口，用于承载自动化流程
 * @author xiexu
 * @since 2026-01-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MCPServerCSDNJob {

    private final ChatClientAssemblyService chatClientAssemblyService;
    private final ChatMemory chatMemory;
    private final ChatMemoryRepository chatMemoryRepository;

    /**
     * traceId 传递指令模板
     */
    private static final String TRACE_ID_SYSTEM_PROMPT = """
            [链路追踪指令]
            当前请求的 traceId 为: %s
            在调用任何工具时，请将此 traceId 作为参数传递（参数名: traceId）。
            这用于端到端日志关联，请务必传递。
            """;

    /**
     * CSDN 文章发布与微信通知
     * XXL-Job Handler: mcpServerCSDNHandler
     * 建议 Cron: 0 0 10,11,15,16 * * ? (每天 10:00、11:00、15:00、16:00 执行)
     *
     * 为什么：将内容生产集中到固定时间点，便于运营节奏与资源控制。
     */
    @XxlJob("mcpServerCSDNHandler")
    public void mcpServerCSDNHandler() {
        String conversationId = TraceIdUtils.getOrCreateTraceId();
        PromptChatMemoryAdvisor memoryAdvisor = PromptChatMemoryAdvisor
                .builder(chatMemory)
                .conversationId(conversationId)
                .build();
        
        ChatClient chatClient = chatClientAssemblyService
                .buildDefaultChatClient(memoryAdvisor);
        String systemPrompt = String
                .format(TRACE_ID_SYSTEM_PROMPT, conversationId);

        
        String publishPrompt = """
                我需要你帮我生成一篇文章，要求如下：
                1. 场景为 AI 学习与实战系列文章
                2. 主题从以下列表中任选其一深入讲解，不要全部覆盖：Spring AI + MCP 实战、RAG 入门、向量数据库实践、Skills 实战、Prompt Engineering、Embedding、微调与对齐（SFT/RLHF）、评测与安全、MLOps/上线、GPU/推理优化
                3. 文章结构清晰，按主题分小节，循序渐进，从概念、原理、关键步骤、实践示例与注意事项进行讲解
                4. 至少包含 8 个小节，每个小节不少于 400 字，全文不少于 5000 字
                5. 结尾给出学习路线与实践建议，便于新手跟学

                根据以上内容，不要阐述其他信息，请直接提供：文章标题、文章内容、文章标签（最多7个，用英文逗号隔开）、文章简述（100字）

                将以上内容发布文章到CSDN。
                """;

        try {
            log.info("开始生成并发布 CSDN 文章");
            String publishResult = chatClient
                    .prompt()
                    .system(systemPrompt)
                    .user(publishPrompt)
                    .call()
                    .content();
            log.info("CSDN 文章发布结果: {}", publishResult);

            
            String noticePrompt = """
                    根据上一轮对话中已发布的文章信息，进行微信公众号消息通知：
                    - 平台：CSDN
                    - 主题：使用已发布的文章标题
                    - 描述：使用已发布的文章简述
                    - 跳转地址：使用已发布文章返回的 url

                    注意：不要再次发布文章，直接使用上一轮对话中的发布结果。
                    """;

            log.info("开始发送微信通知");
            String noticeResult = chatClient
                    .prompt()
                    .system(systemPrompt)
                    .user(noticePrompt)
                    .call()
                    .content();
            log.info("微信通知结果: {}", noticeResult);
            log.info("CSDN 定时任务执行完成");
        } finally {
            chatMemoryRepository.deleteByConversationId(conversationId);
            log.info("已清理 CSDN 任务会话记忆: {}", conversationId);
        }
    }

}
