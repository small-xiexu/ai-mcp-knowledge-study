package com.xbk.knowledge.infrastructure.mapper;

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
     * @param session 会话实体
     * @return 影响行数
     */
    int insertSession(ChatSession session);

    /**
     * 更新会话
     *
     * @param session 会话实体
     * @return 影响行数
     */
    int updateSession(ChatSession session);

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     * @return 影响行数
     */
    int deleteById(Long sessionId);

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
     * @param query 分页条件
     * @return 会话列表
     */
    List<ChatSession> findPage(ChatSessionPageQuery query);

    /**
     * 统计会话总数
     *
     * @return 总数
     */
    long countAll();
}
