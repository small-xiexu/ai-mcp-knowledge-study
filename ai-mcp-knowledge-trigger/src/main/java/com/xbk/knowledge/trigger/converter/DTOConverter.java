package com.xbk.knowledge.trigger.converter;

import com.xbk.knowledge.api.dto.ai.AIRequest;
import com.xbk.knowledge.api.dto.ai.AIRequestMedia;
import com.xbk.knowledge.application.model.dto.AICallMedia;
import com.xbk.knowledge.application.model.dto.AICallCommand;

import java.util.List;
import java.util.Map;

/**
 * DTO 转换工具类
 * 负责应用层与 API DTO 的转换（接口适配层职责）
 *
 * 职责：接口层 DTO 转换，用于隔离传输与领域模型
 *
 * @author sxie
 */
public class DTOConverter {

    /**
     * API AIRequest -> 应用层 AICallCommand
     *
     * 接口层只做字段映射，避免引入业务逻辑导致边界污染。
     * 
     * @param api AI 请求参数。
     * @return 转换后的 AI 调用命令。
     */
    public static AICallCommand toAppAICallCommand(AIRequest api) {
        if (api == null) {
            return null;
        }
        // 请求内容
        String content = api.getContent();
        // 系统提示词
        String systemPrompt = api.getSystemPrompt();
        // 模型参数
        Map<String, Object> parameters = api.getParameters();
        // 流式返回
        Boolean streaming = api.getStreaming();
        // 模型ID
        Long modelId = api.getModelId();
        // 会话ID
        Long sessionId = api.getSessionId();
        // RAG标签
        List<String> ragTags = api.getRagTags();
        // 媒体输入
        List<AIRequestMedia> mediaList = api.getMediaList();
        List<AICallMedia> commandMediaList = mediaList == null ? null : mediaList.stream()
                .map(media -> AICallMedia.builder()
                        .kind(media.getKind())
                        .name(media.getName())
                        .mimeType(media.getMimeType())
                        .data(media.getData())
                        .text(media.getText())
                        .build())
                .toList();
        return AICallCommand.builder()
                .content(content)
                .systemPrompt(systemPrompt)
                .parameters(parameters)
                .streaming(streaming)
                .modelId(modelId)
                .sessionId(sessionId)
                .ragTags(ragTags)
                .mediaList(commandMediaList)
                .build();
    }

}
