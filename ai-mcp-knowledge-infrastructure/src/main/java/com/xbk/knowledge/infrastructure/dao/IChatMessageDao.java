package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.ChatMessagePO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.chat.model.valobj.ChatMessagePageQuery;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息 Mapper
 * 使用 XML 执行 SQL，避免注解或默认方法绕过约束
 *
 * 职责：MyBatis Mapper 接口，用于映射数据库操作
 *
 * @author sxie
 */
@Mapper
public interface IChatMessageDao extends BaseMapper<ChatMessagePO> {

    /**
     * 新增消息
     *
     * 落库对话消息
     * 
     * @param message 待写入的消息持久化实体。
     * @return 影响行数。
     */
    int insertMessage(ChatMessagePO message);

    /**
     * 分页查询会话消息
     *
     * 控制单次返回数量
     * 
     * @param query 分页查询条件。
     * @return ChatMessagePO 列表。
     */
    List<ChatMessagePO> findPage(ChatMessagePageQuery query);

    /**
     * 统计会话消息总数
     *
     * 分页展示需要总数
     * 
     * @param sessionId 会话 ID。
     * @return 统计数量。
     */
    long countBySessionId(Long sessionId);

    /**
     * 删除会话消息
     *
     * 清理会话历史消息
     * 
     * @param sessionId 会话 ID。
     * @return 影响行数。
     */
    int deleteBySessionId(Long sessionId);

    /**
     * 删除过期会话的消息
     *
     * 清理历史数据，控制规模
     * 
     * @param updatedBefore 会话更新时间上限（早于该时间的消息会被删除）。
     * @return 影响行数。
     */
    int deleteBySessionUpdatedBefore(LocalDateTime updatedBefore);
}
