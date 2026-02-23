package com.xbk.knowledge.infrastructure.repository.chat;

import com.xbk.knowledge.domain.chat.model.entity.ChatMessage;
import com.xbk.knowledge.domain.chat.model.valobj.ChatMessagePageQuery;
import com.xbk.knowledge.domain.chat.adapter.repository.ChatMessageRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IChatMessageDao;
import com.xbk.knowledge.infrastructure.dao.po.ChatMessagePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息仓储实现
 *
 * 职责：消息数据持久化访问
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepository {

    private final IChatMessageDao chatMessageMapper;

    /**
     * 创建消息
     *
     * 为什么：落库时补齐时间戳，保证审计字段一致
     * 入参：消息实体
     * 出参：持久化后的消息
     */
    @Override
    public ChatMessage create(ChatMessage message) {
        // 基础设施层统一维护时间戳
        message.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insertMessage(BeanMappingUtils.map(message, ChatMessagePO.class));
        return message;
    }

    /**
     * 分页查询会话消息
     *
     * 为什么：控制单次返回数量
     * 入参：分页查询条件
     * 出参：消息列表
     */
    @Override
    public List<ChatMessage> findPage(ChatMessagePageQuery query) {
        return BeanMappingUtils.mapList(chatMessageMapper.findPage(query), ChatMessage.class);
    }

    /**
     * 统计会话消息总数
     *
     * 为什么：分页展示需要总数
     * 入参：会话 ID
     * 出参：总数
     */
    @Override
    public long countBySessionId(Long sessionId) {
        return chatMessageMapper.countBySessionId(sessionId);
    }

    /**
     * 删除会话消息
     *
     * 为什么：清理指定会话历史消息
     * 入参：会话 ID
     * 出参：无
     */
    @Override
    public void deleteBySessionId(Long sessionId) {
        chatMessageMapper.deleteBySessionId(sessionId);
    }

    /**
     * 删除过期会话的消息
     * <p>
     * 为什么：清理历史数据，控制规模
     * 入参：截止时间
     * 出参：删除数量
     */
    @Override
    public void deleteBySessionUpdatedBefore(LocalDateTime updatedBefore) {
        chatMessageMapper.deleteBySessionUpdatedBefore(updatedBefore);
    }
}
