package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.ChatSessionAppService;
import com.xbk.knowledge.domain.model.entity.ChatMessage;
import com.xbk.knowledge.domain.model.entity.ChatSession;
import com.xbk.knowledge.domain.model.vo.chat.ChatMessagePageQuery;
import com.xbk.knowledge.domain.model.vo.chat.ChatSessionPageQuery;
import com.xbk.knowledge.domain.repository.ChatMessageRepository;
import com.xbk.knowledge.domain.repository.ChatSessionRepository;
import com.xbk.knowledge.types.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 聊天会话应用服务实现
 * 负责会话与消息相关用例编排
 *
 * 职责：应用层用例实现，用于协调领域能力
 *
 * @author xiexu
 */
@Service
@RequiredArgsConstructor
public class ChatSessionAppServiceImpl implements ChatSessionAppService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSession createSession(ChatSession session) {
        return chatSessionRepository.create(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSession updateSession(ChatSession session) {
        return chatSessionRepository.update(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long sessionId) {
        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.deleteById(sessionId);
    }

    @Override
    public ChatSession getSession(Long sessionId) {
        return chatSessionRepository.findById(sessionId);
    }

    @Override
    public PageResult<ChatSession> listSessions(int pageNum, int pageSize) {
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        ChatSessionPageQuery query = new ChatSessionPageQuery(offset, pageSize);
        List<ChatSession> sessions = chatSessionRepository.findPage(query);
        long total = chatSessionRepository.countAll();
        return PageResult.of(sessions, total, pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage appendMessage(ChatMessage message) {
        return chatMessageRepository.create(message);
    }

    @Override
    public PageResult<ChatMessage> listMessages(Long sessionId, int pageNum, int pageSize) {
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        ChatMessagePageQuery query = new ChatMessagePageQuery(sessionId, offset, pageSize);
        List<ChatMessage> messages = chatMessageRepository.findPage(query);
        long total = chatMessageRepository.countBySessionId(sessionId);
        return PageResult.of(messages, total, pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessages(Long sessionId) {
        chatMessageRepository.deleteBySessionId(sessionId);
    }

}
