package com.xbk.knowledge.infrastructure.mapper.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.ChatSession;
import com.xbk.knowledge.domain.model.vo.chat.ChatSessionPageQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 聊天会话 Mapper
 * 使用 XML 执行 SQL，避免注解或默认方法绕过约束
 *
 * 职责：MyBatis Mapper 接口，用于映射数据库操作
 *
 * @author xiexu
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    /**
     * 新增会话
     *
     * 为什么：落库会话记录
     * 入参：会话实体
     * 出参：影响行数
     */
    int insertSession(ChatSession session);

    /**
     * 更新会话
     *
     * 为什么：更新会话元数据
     * 入参：会话实体
     * 出参：影响行数
     */
    int updateSession(ChatSession session);

    /**
     * 删除会话
     *
     * 为什么：清理会话记录
     * 入参：会话ID
     * 出参：影响行数
     */
    int deleteById(Long sessionId);

    /**
     * 根据ID查询会话
     *
     * 为什么：按唯一 ID 获取会话
     * 入参：会话ID
     * 出参：会话实体
     */
    ChatSession findById(Long sessionId);

    /**
     * 分页查询会话
     *
     * 为什么：控制单次返回数量
     * 入参：分页条件
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
     * 为什么：清理历史会话
     * 入参：截止时间
     * 出参：影响行数
     */
    int deleteByUpdatedBefore(java.time.LocalDateTime updatedBefore);
}
