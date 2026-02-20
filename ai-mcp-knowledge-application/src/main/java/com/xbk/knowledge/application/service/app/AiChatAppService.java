package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.AICallResult;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * AI 对话应用服务接口
 * 提供同步与流式对话能力，支持 RAG
 *
 * 职责：应用层用例接口，用于封装调用入口
 * @author sxie
 */
public interface AiChatAppService {

    /**
     * 同步对话
     *
     * @param command 请求
     * @return 结果
     */
    AICallResult chat(AICallCommand command);

    /**
     * 流式对话
     *
     * @param command 请求
     * @return 流式响应
     */
    Flux<ChatResponse> streamChat(AICallCommand command);
}
