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
 * 负责会话与消息的增删改查，作为 HTTP 适配层隔离前端协议与领域模型
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
     *
     * 为什么：会话是消息聚合的根，先创建会话再追加消息，保证消息归属清晰
     * 入参：会话标题、模型、RAG 标签
     * 出参：创建后的会话信息
     */
    @PostMapping
    public Result<ChatSessionResponse> createSession(@RequestBody ChatSessionCreateRequest request) {
        /*
         * 目的：将请求 DTO 转为领域实体，保持领域层不依赖接口层结构
         */
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
     *
     * 为什么：前端只允许修改标题、模型与标签，不在此处处理消息集合
     * 入参：会话 ID + 更新字段
     * 出参：更新后的会话信息
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
     *
     * 为什么：由应用层统一处理会话级联逻辑（如消息清理）
     * 入参：会话 ID
     * 出参：删除结果
     */
    @PostMapping("/delete")
    public Result<Void> deleteSession(@RequestBody IdRequest request) {
        Long id = request.getId();
        chatSessionAppService.deleteSession(id);
        return Result.success(null);
    }

    /**
     * 查询会话详情
     *
     * 为什么：提供前端进入会话时的详情数据
     * 入参：会话 ID
     * 出参：会话详情
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
     *
     * 为什么：统一分页协议，避免一次性拉取全部会话导致响应变大
     * 入参：分页参数
     * 出参：分页后的会话列表
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
     *
     * 为什么：消息写入需要绑定会话 ID，保持会话上下文连续
     * 入参：会话 ID + 消息内容
     * 出参：保存后的消息
     */
    @PostMapping("/{id}/messages")
    public Result<ChatMessageResponse> appendMessage(@PathVariable("id") Long id,
                                                     @RequestBody ChatMessageCreateRequest request) {
        /*
         * 目的：从请求构建领域消息，防止接口层字段直接流入持久层
         */
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
     *
     * 为什么：避免一次加载过多历史消息导致性能问题
     * 入参：会话 ID + 分页参数
     * 出参：分页后的消息列表
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
     *
     * 为什么：提供会话级清理能力，避免影响会话元数据
     * 入参：会话 ID
     * 出参：清理结果
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
        /*
         * 目的：将存储的 JSON 字符串还原为标签列表，前端无需自行解析
         * 约束：解析失败时回退为空列表，避免影响主流程
         */
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
        /*
         * 目的：存储时统一序列化为 JSON，便于数据库索引与查询
         * 约束：序列化失败时回退为空数组，保持字段结构稳定
         */
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    /**
     * 会话消息分页查询请求
     *
     * 为什么：列表查询统一走 POST 以便扩展过滤字段且避免 URL 过长
     * 入参：会话 ID + 分页参数
     * 出参：用于应用层分页查询
     */
    private static class ChatMessagePageRequest extends PageRequest {
        
        private Long sessionId;

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }
    }
}
