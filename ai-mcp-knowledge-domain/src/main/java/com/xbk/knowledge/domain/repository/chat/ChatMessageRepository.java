package com.xbk.knowledge.domain.repository.chat;

import com.xbk.knowledge.domain.model.entity.ChatMessage;
import com.xbk.knowledge.domain.model.vo.chat.ChatMessagePageQuery;

import java.time.LocalDateTime;
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
     * 为什么：持久化会话消息，支持历史记录
     * 入参：消息实体
     * 出参：持久化后的消息
     */
    ChatMessage create(ChatMessage message);

    /**
     * 分页查询会话消息
     *
     * 为什么：控制单次返回数量，避免响应过大
     * 入参：分页查询条件
     * 出参：消息列表
     */
    List<ChatMessage> findPage(ChatMessagePageQuery query);

    /**
     * 统计会话消息总数
     *
     * 为什么：分页展示需要总数
     * 入参：会话 ID
     * 出参：总数
     */
    long countBySessionId(Long sessionId);

    /**
     * 删除会话下的全部消息
     *
     * 为什么：清理指定会话历史消息
     * 入参：会话 ID
     * 出参：无
     */
    void deleteBySessionId(Long sessionId);

    /**
     * 删除过期会话的消息
     * <p>
     * 为什么：清理历史消息，控制数据规模
     * 入参：截止时间
     * 出参：删除行数
     */
    void deleteBySessionUpdatedBefore(LocalDateTime updatedBefore);
}
