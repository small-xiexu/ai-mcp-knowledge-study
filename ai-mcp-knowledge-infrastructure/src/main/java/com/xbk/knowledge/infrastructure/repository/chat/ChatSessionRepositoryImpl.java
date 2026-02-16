package com.xbk.knowledge.infrastructure.repository.chat;

import com.xbk.knowledge.domain.model.entity.ChatSession;
import com.xbk.knowledge.domain.model.vo.chat.ChatSessionPageQuery;
import com.xbk.knowledge.domain.repository.chat.ChatSessionRepository;
import com.xbk.knowledge.infrastructure.mapper.chat.ChatSessionMapper;
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

    /**
     * 创建会话
     *
     * 为什么：落库时补齐时间戳，保证审计字段一致
     * 入参：会话实体
     * 出参：持久化后的会话
     */
    @Override
    public ChatSession create(ChatSession session) {
        /*
         * 目的：基础设施层统一维护时间戳，避免上层重复设置
         */
        LocalDateTime now = LocalDateTime.now();
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        chatSessionMapper.insertSession(session);
        return session;
    }

    /**
     * 更新会话
     *
     * 为什么：更新时刷新更新时间，保持审计一致
     * 入参：会话实体
     * 出参：更新后的会话
     */
    @Override
    public ChatSession update(ChatSession session) {
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.updateSession(session);
        return session;
    }

    /**
     * 删除会话
     *
     * 为什么：按 ID 删除会话记录
     * 入参：会话 ID
     * 出参：无
     */
    @Override
    public void deleteById(Long sessionId) {
        chatSessionMapper.deleteById(sessionId);
    }

    /**
     * 查询会话
     *
     * 为什么：获取会话详情
     * 入参：会话 ID
     * 出参：会话实体
     */
    @Override
    public ChatSession findById(Long sessionId) {
        return chatSessionMapper.findById(sessionId);
    }

    /**
     * 分页查询会话
     *
     * 为什么：控制单次返回数量
     * 入参：分页查询条件
     * 出参：会话列表
     */
    @Override
    public List<ChatSession> findPage(ChatSessionPageQuery query) {
        return chatSessionMapper.findPage(query);
    }

    /**
     * 统计会话总数
     *
     * 为什么：分页展示需要总数
     * 入参：无
     * 出参：总数
     */
    @Override
    public long countAll() {
        return chatSessionMapper.countAll();
    }

    /**
     * 删除过期会话
     *
     * 为什么：清理历史会话，控制数据规模
     * 入参：截止时间
     * 出参：删除数量
     */
    @Override
    public int deleteByUpdatedBefore(LocalDateTime updatedBefore) {
        return chatSessionMapper.deleteByUpdatedBefore(updatedBefore);
    }
}
