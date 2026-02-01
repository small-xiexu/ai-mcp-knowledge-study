package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.entity.ChatMessage;
import com.xbk.knowledge.domain.model.vo.chat.ChatMessagePageQuery;

import java.util.List;

/**
 * 聊天消息仓储接口
 *
 * 职责：消息数据持久化访问
 *
 * @author xiexu
 */
public interface ChatMessageRepository {

    /**
     * 创建消息
     *
     * @param message 消息实体
     * @return 消息实体
     */
    ChatMessage create(ChatMessage message);

    /**
     * 分页查询会话消息
     *
     * @param query 分页查询条件
     * @return 消息列表
     */
    List<ChatMessage> findPage(ChatMessagePageQuery query);

    /**
     * 统计会话消息总数
     *
     * @param sessionId 会话ID
     * @return 总数
     */
    long countBySessionId(Long sessionId);

    /**
     * 删除会话下的全部消息
     *
     * @param sessionId 会话ID
     */
    void deleteBySessionId(Long sessionId);

    /**
     * 删除过期会话的消息
     *
     * @param updatedBefore 截止时间
     * @return 删除行数
     */
    int deleteBySessionUpdatedBefore(java.time.LocalDateTime updatedBefore);
}
