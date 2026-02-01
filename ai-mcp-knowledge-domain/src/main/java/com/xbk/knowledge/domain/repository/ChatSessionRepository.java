package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.entity.ChatSession;
import com.xbk.knowledge.domain.model.vo.chat.ChatSessionPageQuery;

import java.util.List;

/**
 * 聊天会话仓储接口
 *
 * 职责：会话数据持久化访问
 *
 * @author xiexu
 */
public interface ChatSessionRepository {

    /**
     * 创建会话
     *
     * @param session 会话实体
     * @return 会话实体
     */
    ChatSession create(ChatSession session);

    /**
     * 更新会话
     *
     * @param session 会话实体
     * @return 会话实体
     */
    ChatSession update(ChatSession session);

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     */
    void deleteById(Long sessionId);

    /**
     * 根据ID查询会话
     *
     * @param sessionId 会话ID
     * @return 会话实体
     */
    ChatSession findById(Long sessionId);

    /**
     * 分页查询会话
     *
     * @param query 分页查询条件
     * @return 会话列表
     */
    List<ChatSession> findPage(ChatSessionPageQuery query);

    /**
     * 统计会话总数
     *
     * @return 总数
     */
    long countAll();

    /**
     * 删除过期会话
     *
     * @param updatedBefore 截止时间
     * @return 删除行数
     */
    int deleteByUpdatedBefore(java.time.LocalDateTime updatedBefore);
}
