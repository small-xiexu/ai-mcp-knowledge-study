package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.ChatSession;
import com.xbk.knowledge.domain.model.vo.chat.ChatSessionPageQuery;
import com.xbk.knowledge.domain.repository.ChatSessionRepository;
import com.xbk.knowledge.infrastructure.mapper.ChatSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天会话仓储实现
 *
 * 职责：会话数据持久化访问
 *
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class ChatSessionRepositoryImpl implements ChatSessionRepository {

    private final ChatSessionMapper chatSessionMapper;

    @Override
    public ChatSession create(ChatSession session) {
        LocalDateTime now = LocalDateTime.now();
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        chatSessionMapper.insertSession(session);
        return session;
    }

    @Override
    public ChatSession update(ChatSession session) {
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.updateSession(session);
        return session;
    }

    @Override
    public void deleteById(Long sessionId) {
        chatSessionMapper.deleteById(sessionId);
    }

    @Override
    public ChatSession findById(Long sessionId) {
        return chatSessionMapper.findById(sessionId);
    }

    @Override
    public List<ChatSession> findPage(ChatSessionPageQuery query) {
        return chatSessionMapper.findPage(query);
    }

    @Override
    public long countAll() {
        return chatSessionMapper.countAll();
    }
}
