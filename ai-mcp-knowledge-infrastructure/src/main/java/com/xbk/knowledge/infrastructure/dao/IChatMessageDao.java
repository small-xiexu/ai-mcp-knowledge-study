package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.ChatMessagePO;
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
public interface IChatMessageDao extends BaseMapper<ChatMessagePO> {

    /**
     * 新增消息
     *
     * 为什么：落库对话消息
     * 入参：消息实体
     * 出参：影响行数
     */
    int insertMessage(ChatMessage message);

    /**
     * 分页查询会话消息
     *
     * 为什么：控制单次返回数量
     * 入参：查询条件
     * 出参：消息列表
     */
    List<ChatMessage> findPage(ChatMessagePageQuery query);

    /**
     * 统计会话消息总数
     *
     * 为什么：分页展示需要总数
     * 入参：会话ID
     * 出参：总数
     */
    long countBySessionId(Long sessionId);

    /**
     * 删除会话消息
     *
     * 为什么：清理会话历史消息
     * 入参：会话ID
     * 出参：影响行数
     */
    int deleteBySessionId(Long sessionId);

    /**
     * 删除过期会话的消息
     *
     * 为什么：清理历史数据，控制规模
     * 入参：截止时间
     * 出参：影响行数
     */
    int deleteBySessionUpdatedBefore(java.time.LocalDateTime updatedBefore);
}
