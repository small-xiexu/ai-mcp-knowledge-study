package com.xbk.knowledge.domain.chat.adapter.repository;

import com.xbk.knowledge.domain.chat.model.entity.ChatSession;
import com.xbk.knowledge.domain.chat.model.valobj.ChatSessionPageQuery;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天会话仓储接口
 *
 * 职责：会话数据持久化访问
 *
 * @author sxie
 */
public interface ChatSessionRepository {

    /**
     * 创建会话。
     * <p>
     * 持久化会话聚合根，便于后续追加消息。
     * 
     * @param session 待创建的会话实体。
     * @return 已持久化的会话实体。
     */
    ChatSession create(ChatSession session);

    /**
     * 更新会话。
     * <p>
     * 更新会话元数据，保持会话状态一致性。
     * 
     * @param session 待更新的会话实体。
     * @return 更新后的会话实体。
     */
    ChatSession update(ChatSession session);

    /**
     * 删除会话。
     * <p>
     * 清理会话聚合根。
     * 
     * @param sessionId 会话 ID。
     */
    void deleteById(Long sessionId);

    /**
     * 根据 ID 查询会话。
     * <p>
     * 用于加载会话详情。
     * 
     * @param sessionId 会话 ID。
     * @return 会话实体。
     */
    ChatSession findById(Long sessionId);

    /**
     * 分页查询会话。
     * <p>
     * 控制单次返回数量，避免响应过大。
     * 
     * @param query 分页查询条件。
     * @return 会话分页数据列表。
     */
    List<ChatSession> findPage(ChatSessionPageQuery query);

    /**
     * 统计会话总数。
     * <p>
     * 为分页展示提供总记录数。
     * 
     * @return 统计数量。
     */
    long countAll();

    /**
     * 删除过期会话。
     * <p>
     * 清理历史会话，控制数据规模。
     * 
     * @param updatedBefore 最后更新时间上限（早于该时间的会话会被删除）。
     * @return 影响行数。
     */
    int deleteByUpdatedBefore(LocalDateTime updatedBefore);

    /**
     * 查询过期会话 ID 列表。
     * <p>
     * 用于在批量清理前同步处理关联缓存。
     *
     * @param updatedBefore 最后更新时间上限（早于该时间的会话会被识别为过期）。
     * @return 过期会话 ID 列表。
     */
    List<Long> findIdsByUpdatedBefore(LocalDateTime updatedBefore);
}
