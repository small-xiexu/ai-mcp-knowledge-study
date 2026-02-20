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
     * 创建会话
     *
     * 为什么：持久化会话聚合根，便于后续追加消息
     * 入参：会话实体
     * 出参：持久化后的会话
     */
    ChatSession create(ChatSession session);

    /**
     * 更新会话
     *
     * 为什么：更新会话元数据，保持一致性
     * 入参：会话实体
     * 出参：更新后的会话
     */
    ChatSession update(ChatSession session);

    /**
     * 删除会话
     *
     * 为什么：清理会话聚合根
     * 入参：会话 ID
     * 出参：无
     */
    void deleteById(Long sessionId);

    /**
     * 根据ID查询会话
     *
     * 为什么：用于会话详情加载
     * 入参：会话 ID
     * 出参：会话实体
     */
    ChatSession findById(Long sessionId);

    /**
     * 分页查询会话
     *
     * 为什么：控制单次返回数量，避免响应过大
     * 入参：分页查询条件
     * 出参：会话列表
     */
    List<ChatSession> findPage(ChatSessionPageQuery query);

    /**
     * 统计会话总数
     *
     * 为什么：分页展示需要总数
     * 入参：无
     * 出参：总数
     */
    long countAll();

    /**
     * 删除过期会话
     *
     * 为什么：清理历史会话，控制数据规模
     * 入参：截止时间
     * 出参：删除行数
     */
    int deleteByUpdatedBefore(LocalDateTime updatedBefore);
}
