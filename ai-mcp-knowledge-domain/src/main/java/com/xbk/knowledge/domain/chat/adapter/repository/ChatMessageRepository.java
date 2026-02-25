package com.xbk.knowledge.domain.chat.adapter.repository;

import com.xbk.knowledge.domain.chat.model.entity.ChatMessage;
import com.xbk.knowledge.domain.chat.model.valobj.ChatMessagePageQuery;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息仓储接口
 *
 * 职责：消息数据持久化访问
 *
 * @author sxie
 */
public interface ChatMessageRepository {

    /**
     * 创建消息。
     * <p>
     * 持久化会话消息，支持历史记录。
     * 
     * @param message 待创建的消息实体。
     * @return 已持久化的消息实体。
     */
    ChatMessage create(ChatMessage message);

    /**
     * 分页查询会话消息。
     * <p>
     * 控制单次返回数量，避免响应过大。
     * 
     * @param query 分页查询条件。
     * @return 消息分页数据列表。
     */
    List<ChatMessage> findPage(ChatMessagePageQuery query);

    /**
     * 统计会话消息总数。
     * <p>
     * 为分页展示提供总记录数。
     * 
     * @param sessionId 会话 ID。
     * @return 统计数量。
     */
    long countBySessionId(Long sessionId);

    /**
     * 删除会话下的全部消息。
     * <p>
     * 清理指定会话的历史消息。
     * 
     * @param sessionId 会话 ID。
     */
    void deleteBySessionId(Long sessionId);

    /**
     * 删除过期会话的消息
     * <p>
     * 清理历史消息，控制数据规模。
     * 
     * @param updatedBefore 会话更新时间上限（早于该时间的消息会被删除）。
     */
    void deleteBySessionUpdatedBefore(LocalDateTime updatedBefore);
}
