package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.ai.ChatMessageCreateRequest;
import com.xbk.knowledge.api.dto.ai.ChatMessagePageRequest;
import com.xbk.knowledge.api.dto.ai.ChatMessageResponse;
import com.xbk.knowledge.api.dto.ai.ChatSessionCreateRequest;
import com.xbk.knowledge.api.dto.ai.ChatSessionResponse;
import com.xbk.knowledge.api.dto.ai.ChatSessionUpdateRequest;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.types.common.PageRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;

/**
 * 会话管理服务接口
 * 定义 AI 会话与消息管理的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IChatSessionService {

    /**
     * 创建会话。
     *
     * @param request 请求参数
     * @return 创建结果
     */
    Result<ChatSessionResponse> createSession(ChatSessionCreateRequest request);

    /**
     * 更新会话信息。
     *
     * @param request 请求参数
     * @return 更新结果
     */
    Result<ChatSessionResponse> updateSession(ChatSessionUpdateRequest request);

    /**
     * 删除会话。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<Void> deleteSession(IdRequest request);

    /**
     * 查询会话详情。
     *
     * @param request 请求参数
     * @return 查询结果
     */
    Result<ChatSessionResponse> getSession(IdRequest request);

    /**
     * 分页查询会话列表。
     *
     * @param request 请求参数
     * @return 列表结果
     */
    Result<PageResult<ChatSessionResponse>> listSessions(PageRequest request);

    /**
     * 追加会话消息。
     *
     * @param id 主键 ID
     * @param request 请求参数
     * @return 处理结果
     */
    Result<ChatMessageResponse> appendMessage(Long id, ChatMessageCreateRequest request);

    /**
     * 分页查询消息列表。
     *
     * @param request 请求参数
     * @return 列表结果
     */
    Result<PageResult<ChatMessageResponse>> listMessages(ChatMessagePageRequest request);

    /**
     * 删除会话消息。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<Void> deleteMessages(IdRequest request);
}
