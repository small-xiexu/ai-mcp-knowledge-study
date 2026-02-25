package com.xbk.knowledge.infrastructure.repository.chat;

import com.xbk.knowledge.domain.chat.model.entity.ChatSession;
import com.xbk.knowledge.domain.chat.model.valobj.ChatSessionPageQuery;
import com.xbk.knowledge.domain.chat.adapter.repository.ChatSessionRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IChatSessionDao;
import com.xbk.knowledge.infrastructure.dao.po.ChatSessionPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天会话仓储实现
 *
 * 职责：会话数据持久化访问
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class ChatSessionRepositoryImpl implements ChatSessionRepository {

    /**
     * 聊天会话数据访问对象。
     */
    private final IChatSessionDao chatSessionMapper;

    /**
     * 创建会话
     *
     * 落库时补齐时间戳，保证审计字段一致
     * 
     * @param session 待创建的会话实体。
     * @return 已持久化的会话实体。
     */
    @Override
    public ChatSession create(ChatSession session) {
        // 基础设施层统一维护时间戳，避免上层重复设置
        LocalDateTime now = LocalDateTime.now();
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        chatSessionMapper.insertSession(BeanMappingUtils.map(session, ChatSessionPO.class));
        return session;
    }

    /**
     * 更新会话
     *
     * 更新时刷新更新时间，保持审计一致
     * 
     * @param session 待更新的会话实体。
     * @return 更新后的会话实体。
     */
    @Override
    public ChatSession update(ChatSession session) {
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.updateSession(BeanMappingUtils.map(session, ChatSessionPO.class));
        return session;
    }

    /**
     * 删除会话
     *
     * 按 ID 删除会话记录
     * 
     * @param sessionId 会话 ID。
     */
    @Override
    public void deleteById(Long sessionId) {
        chatSessionMapper.deleteById(sessionId);
    }

    /**
     * 查询会话
     *
     * 获取会话详情
     * 
     * @param sessionId 会话 ID。
     * @return 会话实体。
     */
    @Override
    public ChatSession findById(Long sessionId) {
        return BeanMappingUtils.map(chatSessionMapper.findById(sessionId), ChatSession.class);
    }

    /**
     * 分页查询会话
     *
     * 控制单次返回数量
     * 
     * @param query 分页查询条件。
     * @return 会话列表。
     */
    @Override
    public List<ChatSession> findPage(ChatSessionPageQuery query) {
        return BeanMappingUtils.mapList(chatSessionMapper.findPage(query), ChatSession.class);
    }

    /**
     * 统计会话总数
     *
     * 分页展示需要总数
     * 
     * @return 统计数量。
     */
    @Override
    public long countAll() {
        return chatSessionMapper.countAll();
    }

    /**
     * 删除过期会话
     *
     * 清理历史会话，控制数据规模
     * 
     * @param updatedBefore 会话更新时间上限（早于该时间的会话会被删除）。
     * @return 影响行数。
     */
    @Override
    public int deleteByUpdatedBefore(LocalDateTime updatedBefore) {
        return chatSessionMapper.deleteByUpdatedBefore(updatedBefore);
    }

    /**
     * 查询过期会话 ID 列表
     *
     * 用于在批量删除前清理会话记忆缓存
     *
     * @param updatedBefore 会话更新时间上限（早于该时间的会话会被识别为过期）。
     * @return 过期会话 ID 列表。
     */
    @Override
    public List<Long> findIdsByUpdatedBefore(LocalDateTime updatedBefore) {
        return chatSessionMapper.findIdsByUpdatedBefore(updatedBefore);
    }
}
