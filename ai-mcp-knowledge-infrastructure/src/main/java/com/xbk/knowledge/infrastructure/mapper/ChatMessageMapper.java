package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.ChatMessage;
import com.xbk.knowledge.domain.model.vo.chat.ChatMessagePageQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 聊天消息 Mapper
 * 使用 XML 执行 SQL，避免注解或默认方法绕过约束
 *
 * 职责：MyBatis Mapper 接口，用于映射数据库操作
 *
 * @author xiexu
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 新增消息
     *
     * @param message 消息实体
     * @return 影响行数
     */
    int insertMessage(ChatMessage message);

    /**
     * 分页查询会话消息
     *
     * @param query 查询条件
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
     * 删除会话消息
     *
     * @param sessionId 会话ID
     * @return 影响行数
     */
    int deleteBySessionId(Long sessionId);

    /**
     * 删除过期会话的消息
     *
     * @param updatedBefore 截止时间
     * @return 影响行数
     */
    int deleteBySessionUpdatedBefore(java.time.LocalDateTime updatedBefore);
}
