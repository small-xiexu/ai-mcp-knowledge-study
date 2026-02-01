package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.dto.ai.ChatMessageCreateRequest;
import com.xbk.knowledge.api.dto.ai.ChatMessageResponse;
import com.xbk.knowledge.api.dto.ai.ChatSessionCreateRequest;
import com.xbk.knowledge.api.dto.ai.ChatSessionResponse;
import com.xbk.knowledge.api.dto.ai.ChatSessionUpdateRequest;
import com.xbk.knowledge.application.service.app.ChatSessionAppService;
import com.xbk.knowledge.domain.model.entity.ChatMessage;
import com.xbk.knowledge.domain.model.entity.ChatSession;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.types.common.PageRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天会话管理 Controller
 * 负责会话与消息的增删改查
 *
 * @author xiexu
 */
@RestController
@RequestMapping("/api/ai/sessions")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionAppService chatSessionAppService;
    private final ObjectMapper objectMapper;

    /**
     * 创建会话
     */
    @PostMapping
    public Result<ChatSessionResponse> createSession(@RequestBody ChatSessionCreateRequest request) {
        ChatSession session = ChatSession.builder()
                .title(request.getTitle())
                .modelId(request.getModelId())
                .ragTags(toRagTagsJson(request.getRagTags()))
                .build();
        ChatSession created = chatSessionAppService.createSession(session);
        return Result.success(toSessionResponse(created));
    }

    /**
     * 更新会话
     */
    @PostMapping("/update")
    public Result<ChatSessionResponse> updateSession(@RequestBody ChatSessionUpdateRequest request) {
        Long id = request.getId();
        ChatSession existing = chatSessionAppService.getSession(id);
        if (existing == null) {
            return Result.error(404, "会话不存在");
        }
        existing.setTitle(request.getTitle());
        existing.setModelId(request.getModelId());
        existing.setRagTags(toRagTagsJson(request.getRagTags()));
        ChatSession updated = chatSessionAppService.updateSession(existing);
        return Result.success(toSessionResponse(updated));
    }

    /**
     * 删除会话
     */
    @PostMapping("/delete")
    public Result<Void> deleteSession(@RequestBody IdRequest request) {
        Long id = request.getId();
        chatSessionAppService.deleteSession(id);
        return Result.success(null);
    }

    /**
     * 查询会话详情
     */
    @PostMapping("/detail")
    public Result<ChatSessionResponse> getSession(@RequestBody IdRequest request) {
        Long id = request.getId();
        ChatSession session = chatSessionAppService.getSession(id);
        if (session == null) {
            return Result.error(404, "会话不存在");
        }
        return Result.success(toSessionResponse(session));
    }

    /**
     * 分页查询会话
     */
    @PostMapping("/list")
    public Result<PageResult<ChatSessionResponse>> listSessions(@RequestBody PageRequest request) {
        PageResult<ChatSession> page = chatSessionAppService.listSessions(request.getPageNum(), request.getPageSize());
        List<ChatSessionResponse> records = page.getRecords()
                .stream()
                .map(this::toSessionResponse)
                .collect(Collectors.toList());
        PageResult<ChatSessionResponse> response = PageResult.of(records, page.getTotal(), page.getPageNum(), page.getPageSize());
        return Result.success(response);
    }

    /**
     * 追加消息
     */
    @PostMapping("/{id}/messages")
    public Result<ChatMessageResponse> appendMessage(@PathVariable("id") Long id,
                                                     @RequestBody ChatMessageCreateRequest request) {
        ChatMessage message = ChatMessage.builder()
                .sessionId(id)
                .role(request.getRole())
                .content(request.getContent())
                .modelId(request.getModelId())
                .promptTokens(request.getPromptTokens())
                .completionTokens(request.getCompletionTokens())
                .totalTokens(request.getTotalTokens())
                .build();
        ChatMessage created = chatSessionAppService.appendMessage(message);
        return Result.success(toMessageResponse(created));
    }

    /**
     * 分页查询会话消息
     */
    @PostMapping("/messages/list")
    public Result<PageResult<ChatMessageResponse>> listMessages(@RequestBody ChatMessagePageRequest request) {
        PageResult<ChatMessage> page = chatSessionAppService.listMessages(
                request.getSessionId(),
                request.getPageNum(),
                request.getPageSize());
        List<ChatMessageResponse> records = page.getRecords()
                .stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
        PageResult<ChatMessageResponse> response = PageResult.of(records, page.getTotal(), page.getPageNum(), page.getPageSize());
        return Result.success(response);
    }

    /**
     * 清空会话消息
     */
    @PostMapping("/messages/delete")
    public Result<Void> deleteMessages(@RequestBody IdRequest request) {
        Long id = request.getId();
        chatSessionAppService.deleteMessages(id);
        return Result.success(null);
    }

    private ChatSessionResponse toSessionResponse(ChatSession session) {
        ChatSessionResponse response = new ChatSessionResponse();
        response.setId(session.getId());
        response.setTitle(session.getTitle());
        response.setModelId(session.getModelId());
        response.setRagTags(parseRagTags(session.getRagTags()));
        response.setCreatedAt(session.getCreatedAt());
        response.setUpdatedAt(session.getUpdatedAt());
        return response;
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(message.getId());
        response.setSessionId(message.getSessionId());
        response.setRole(message.getRole());
        response.setContent(message.getContent());
        response.setModelId(message.getModelId());
        response.setPromptTokens(message.getPromptTokens());
        response.setCompletionTokens(message.getCompletionTokens());
        response.setTotalTokens(message.getTotalTokens());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }

    private List<String> parseRagTags(String rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(rawTags, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    private String toRagTagsJson(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    /**
     * 会话消息分页查询请求
     * 使用 POST 请求体传递分页参数
     */
    private static class ChatMessagePageRequest extends PageRequest {
        /**
         * 会话ID
         */
        private Long sessionId;

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }
    }
}
