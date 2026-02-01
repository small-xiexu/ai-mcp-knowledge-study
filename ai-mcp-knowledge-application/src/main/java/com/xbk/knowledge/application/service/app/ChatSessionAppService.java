package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.model.entity.ChatMessage;
import com.xbk.knowledge.domain.model.entity.ChatSession;
import com.xbk.knowledge.types.common.PageResult;

import java.util.List;

/**
 * 聊天会话应用服务接口
 * 负责会话与消息相关用例编排
 *
 * 职责：应用层用例接口，用于封装调用入口
 *
 * @author xiexu
 */
public interface ChatSessionAppService {

    /**
     * 创建会话
     *
     * @param session 会话实体
     * @return 会话实体
     */
    ChatSession createSession(ChatSession session);

    /**
     * 更新会话
     *
     * @param session 会话实体
     * @return 会话实体
     */
    ChatSession updateSession(ChatSession session);

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     */
    void deleteSession(Long sessionId);

    /**
     * 查询会话
     *
     * @param sessionId 会话ID
     * @return 会话实体
     */
    ChatSession getSession(Long sessionId);

    /**
     * 分页查询会话
     *
     * @param pageNum  当前页
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageResult<ChatSession> listSessions(int pageNum, int pageSize);

    /**
     * 追加消息
     *
     * @param message 消息实体
     * @return 消息实体
     */
    ChatMessage appendMessage(ChatMessage message);

    /**
     * 分页查询会话消息
     *
     * @param sessionId 会话ID
     * @param pageNum   当前页
     * @param pageSize  每页大小
     * @return 分页结果
     */
    PageResult<ChatMessage> listMessages(Long sessionId, int pageNum, int pageSize);

    /**
     * 删除会话消息
     *
     * @param sessionId 会话ID
     */
    void deleteMessages(Long sessionId);
}
