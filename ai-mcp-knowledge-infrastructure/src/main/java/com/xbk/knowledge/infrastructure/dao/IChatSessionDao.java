package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.ChatSessionPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.chat.model.valobj.ChatSessionPageQuery;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天会话 Mapper
 * 使用 XML 执行 SQL，避免注解或默认方法绕过约束
 *
 * 职责：MyBatis Mapper 接口，用于映射数据库操作
 *
 * @author sxie
 */
@Mapper
public interface IChatSessionDao extends BaseMapper<ChatSessionPO> {

    /**
     * 新增会话
     *
     * 落库会话记录
     * 
     * @param session 待写入的会话持久化实体。
     * @return 影响行数。
     */
    int insertSession(ChatSessionPO session);

    /**
     * 更新会话
     *
     * 更新会话元数据
     * 
     * @param session 待更新的会话持久化实体。
     * @return 影响行数。
     */
    int updateSession(ChatSessionPO session);

    /**
     * 删除会话
     *
     * 清理会话记录
     * 
     * @param sessionId 会话 ID。
     * @return 影响行数。
     */
    int deleteById(Long sessionId);

    /**
     * 根据 ID 查询会话
     *
     * 按唯一 ID 获取会话
     * 
     * @param sessionId 会话 ID。
     * @return 会话持久化实体。
     */
    ChatSessionPO findById(Long sessionId);

    /**
     * 分页查询会话
     *
     * 控制单次返回数量
     * 
     * @param query 分页查询条件。
     * @return ChatSessionPO 列表。
     */
    List<ChatSessionPO> findPage(ChatSessionPageQuery query);

    /**
     * 统计会话总数
     *
     * 分页展示需要总数
     * 
     * @return 统计数量。
     */
    long countAll();

    /**
     * 删除过期会话
     *
     * 清理历史会话
     * 
     * @param updatedBefore 会话更新时间上限（早于该时间的会话会被删除）。
     * @return 影响行数。
     */
    int deleteByUpdatedBefore(LocalDateTime updatedBefore);

    /**
     * 查询过期会话 ID 列表
     *
     * 用于批量清理前定位会话缓存
     *
     * @param updatedBefore 会话更新时间上限（早于该时间的会话会被识别为过期）。
     * @return 过期会话 ID 列表。
     */
    List<Long> findIdsByUpdatedBefore(LocalDateTime updatedBefore);
}
