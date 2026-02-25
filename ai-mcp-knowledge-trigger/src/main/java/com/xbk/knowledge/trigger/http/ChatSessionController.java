package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.IChatSessionService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.ai.ChatMessageCreateRequest;
import com.xbk.knowledge.api.dto.ai.ChatMessagePageRequest;
import com.xbk.knowledge.api.dto.ai.ChatMessageResponse;
import com.xbk.knowledge.api.dto.ai.ChatSessionCreateRequest;
import com.xbk.knowledge.api.dto.ai.ChatSessionResponse;
import com.xbk.knowledge.api.dto.ai.ChatSessionUpdateRequest;
import com.xbk.knowledge.application.service.app.ChatSessionAppService;
import com.xbk.knowledge.domain.chat.model.entity.ChatMessage;
import com.xbk.knowledge.domain.chat.model.entity.ChatSession;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.types.common.PageRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageQueryExecutor;
import com.xbk.knowledge.types.common.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * 聊天会话管理 Controller
 * 负责会话与消息的增删改查，作为 HTTP 适配层隔离前端协议与领域模型
 *
 * @author sxie
 */
@RestController
@RequestMapping("/api/ai/sessions")
@RequiredArgsConstructor
public class ChatSessionController implements IChatSessionService {

    /**
     * 会话应用服务。
     */
    private final ChatSessionAppService chatSessionAppService;

    /**
     * JSON 序列化/反序列化组件。
     */
    private final ObjectMapper objectMapper;

    /**
     * 创建会话
     *
     * 会话是消息聚合的根，先创建会话再追加消息，保证消息归属清晰
     * 流程：
     * 1. 进入接口后执行 `agent:write` 权限校验。
     * 2. Spring 完成请求体绑定。
     * 3. Controller 组装 `ChatSession` 并序列化 RAG 标签。
     * 4. 调用 `chatSessionAppService.createSession` 持久化会话。
     * 5. 转换为 `ChatSessionResponse` 并统一返回。
     * 
     * @param request 会话创建请求（标题、模型 ID、RAG 标签）。
     * @return 新建后的会话详情。
     */
    @PostMapping
    @SaCheckPermission("agent:write")
    @Override
    public Result<ChatSessionResponse> createSession(@RequestBody ChatSessionCreateRequest request) {
        // 将请求 DTO 转为领域实体，保持领域层不依赖接口层结构
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
     * 前端只允许修改标题、模型与标签，不在此处处理消息集合
     * 流程：
     * 1. 进入接口后执行 `agent:write` 权限校验。
     * 2. Spring 完成请求体绑定。
     * 3. Controller 先查会话，不存在则直接返回错误。
     * 4. 更新允许变更字段后调用 `chatSessionAppService.updateSession`。
     * 5. 转换为 `ChatSessionResponse` 并统一返回。
     * 
     * @param request 会话更新请求（会话 ID、标题、模型 ID、RAG 标签）。
     * @return 更新后的会话详情。
     */
    @PostMapping("/update")
    @SaCheckPermission("agent:write")
    @Override
    public Result<ChatSessionResponse> updateSession(@RequestBody ChatSessionUpdateRequest request) {
        Long id = request.getId();
        ChatSession existing = chatSessionAppService.getSession(id);
        if (existing == null) {
            return Result.error(404, "话不存在");
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
     * 由应用层统一处理会话级联逻辑（如消息清理）
     * 流程：
     * 1. 进入接口后执行 `agent:write` 权限校验。
     * 2. Spring 完成请求体绑定。
     * 3. Controller 调用 `chatSessionAppService.deleteSession`。
     * 4. 应用层执行会话与关联消息清理。
     * 5. 返回空成功结果。
     * 
     * @param request 会话 ID 请求。
     * @return 删除结果（数据体为空）。
     */
    @PostMapping("/delete")
    @SaCheckPermission("agent:write")
    @Override
    public Result<Void> deleteSession(@RequestBody IdRequest request) {
        Long id = request.getId();
        chatSessionAppService.deleteSession(id);
        return Result.success(null);
    }

    /**
     * 查询会话详情
     *
     * 提供前端进入会话时的详情数据
     * 流程：
     * 1. 进入接口后执行 `agent:read` 权限校验。
     * 2. Spring 完成请求体绑定。
     * 3. Controller 调用 `chatSessionAppService.getSession` 查询详情。
     * 4. 不存在返回错误；存在则转换为 `ChatSessionResponse`。
     * 5. 统一封装结果返回。
     * 
     * @param request 会话 ID 请求。
     * @return 会话详情。
     */
    @PostMapping("/detail")
    @SaCheckPermission("agent:read")
    @Override
    public Result<ChatSessionResponse> getSession(@RequestBody IdRequest request) {
        Long id = request.getId();
        ChatSession session = chatSessionAppService.getSession(id);
        if (session == null) {
            return Result.error(404, "话不存在");
        }
        return Result.success(toSessionResponse(session));
    }

    /**
     * 分页查询会话
     *
     * 统一分页协议，避免一次性拉取全部会话导致响应变大
     * 流程：
     * 1. 进入接口后执行 `agent:read` 权限校验。
     * 2. Spring 完成请求体绑定。
     * 3. Controller 调用 `chatSessionAppService.listSessions` 查询分页数据。
     * 4. 将领域分页记录转换为 `ChatSessionResponse` 列表并重组分页对象。
     * 5. 统一封装 `Result.success` 返回。
     * 
     * @param request 分页请求（页码、页大小）。
     * @return 会话分页结果。
     */
    @PostMapping("/list")
    @SaCheckPermission("agent:read")
    @Override
    public Result<PageResult<ChatSessionResponse>> listSessions(@RequestBody PageRequest request) {
        return PageQueryExecutor.executeByPageNum(
                request == null ? null : request.getPageNum(),
                request == null ? null : request.getPageSize(),
                chatSessionAppService::listSessions,
                this::toSessionResponse
        );
    }

    /**
     * 追加消息
     *
     * 消息写入需要绑定会话 ID，保持会话上下文连续
     * 流程：
     * 1. 进入接口后执行 `agent:write` 权限校验。
     * 2. Spring 完成路径变量与请求体绑定。
     * 3. Controller 组装 `ChatMessage` 领域对象。
     * 4. 调用 `chatSessionAppService.appendMessage` 追加消息。
     * 5. 转换为 `ChatMessageResponse` 并统一返回。
     * 
     * @param id 会话 ID（路径参数）。
     * @param request 消息追加请求（角色、内容、模型 ID）。
     * @return 新增消息详情。
     */
    @PostMapping("/{id}/messages")
    @SaCheckPermission("agent:write")
    @Override
    public Result<ChatMessageResponse> appendMessage(@PathVariable("id") Long id,
                                                     @RequestBody ChatMessageCreateRequest request) {
        // 从请求构建领域消息，防止接口层字段直接流入持久层
        ChatMessage message = ChatMessage.builder()
                .sessionId(id)
                .role(request.getRole())
                .content(request.getContent())
                .modelId(request.getModelId())
                .build();
        ChatMessage created = chatSessionAppService.appendMessage(message);
        return Result.success(toMessageResponse(created));
    }

    /**
     * 分页查询会话消息
     *
     * 避免一次加载过多历史消息导致性能问题
     * 流程：
     * 1. 进入接口后执行 `agent:read` 权限校验。
     * 2. Spring 完成请求体绑定。
     * 3. Controller 调用 `chatSessionAppService.listMessages` 查询分页消息。
     * 4. 将领域消息转换为 `ChatMessageResponse` 并重组分页对象。
     * 5. 统一封装 `Result.success` 返回。
     * 
     * @param request 消息分页请求（会话 ID、页码、页大小）。
     * @return 会话消息分页结果。
     */
    @PostMapping("/messages/list")
    @SaCheckPermission("agent:read")
    @Override
    public Result<PageResult<ChatMessageResponse>> listMessages(@RequestBody ChatMessagePageRequest request) {
        Long sessionId = request == null ? null : request.getSessionId();
        return PageQueryExecutor.executeByPageNum(
                request == null ? null : request.getPageNum(),
                request == null ? null : request.getPageSize(),
                (pageNum, pageSize) -> chatSessionAppService.listMessages(sessionId, pageNum, pageSize),
                this::toMessageResponse
        );
    }

    /**
     * 清空会话消息
     *
     * 提供会话级清理能力，避免影响会话元数据
     * 流程：
     * 1. 进入接口后执行 `agent:write` 权限校验。
     * 2. Spring 完成请求体绑定。
     * 3. Controller 调用 `chatSessionAppService.deleteMessages` 清理消息。
     * 4. 应用层执行会话消息删除，不影响会话主档。
     * 5. 返回空成功结果。
     * 
     * @param request 会话 ID 请求。
     * @return 清理结果（数据体为空）。
     */
    @PostMapping("/messages/delete")
    @SaCheckPermission("agent:write")
    @Override
    public Result<Void> deleteMessages(@RequestBody IdRequest request) {
        Long id = request.getId();
        chatSessionAppService.deleteMessages(id);
        return Result.success(null);
    }

    /**
     * 将会话实体转换为接口层响应对象。
     *
     * @param session 会话实体。
     * @return 会话响应。
     */
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

    /**
     * 将消息实体转换为接口层响应对象。
     *
     * @param message 消息实体。
     * @return 消息响应。
     */
    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(message.getId());
        response.setSessionId(message.getSessionId());
        response.setRole(message.getRole());
        response.setContent(message.getContent());
        response.setModelId(message.getModelId());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }

    /**
     * 解析RAGTags。
     * 
     * @param rawTags 原始标签字符串。
     * @return 标签集合；解析失败时返回空列表。
     */
    private List<String> parseRagTags(String rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return Collections.emptyList();
        }
        // 将存储的 JSON 字符串还原为标签列表，前端无需自行解析
        // 约束解析失败时回退为空列表，避免影响主流程
        try {
            return objectMapper.readValue(rawTags, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    /**
     * 将输入数据转换为RAGTagsJSON。
     * 
     * @param tags 标签列表。
     * @return 标签 JSON 字符串；序列化失败时返回 "[]"。
     */
    private String toRagTagsJson(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "[]";
        }
        // 存储时统一序列化为 JSON，便于数据库索引与查询
        // 约束序列化失败时回退为空数组，保持字段结构稳定
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

}
