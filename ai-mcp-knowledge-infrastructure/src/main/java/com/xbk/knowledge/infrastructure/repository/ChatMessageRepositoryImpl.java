package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.ChatMessage;
import com.xbk.knowledge.domain.model.vo.chat.ChatMessagePageQuery;
import com.xbk.knowledge.domain.repository.ChatMessageRepository;
import com.xbk.knowledge.infrastructure.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息仓储实现
 *
 * 职责：消息数据持久化访问
 *
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepository {

    private final ChatMessageMapper chatMessageMapper;

    @Override
    public ChatMessage create(ChatMessage message) {
        message.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insertMessage(message);
        return message;
    }

    @Override
    public List<ChatMessage> findPage(ChatMessagePageQuery query) {
        return chatMessageMapper.findPage(query);
    }

    @Override
    public long countBySessionId(Long sessionId) {
        return chatMessageMapper.countBySessionId(sessionId);
    }

    @Override
    public void deleteBySessionId(Long sessionId) {
        chatMessageMapper.deleteBySessionId(sessionId);
    }
}
