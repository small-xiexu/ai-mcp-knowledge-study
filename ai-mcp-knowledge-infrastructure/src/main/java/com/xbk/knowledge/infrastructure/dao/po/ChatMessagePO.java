package com.xbk.knowledge.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 聊天消息实体
 * 对应数据库表ai_chat_message
 *
 * 职责：领域实体，用于承载消息记录与统计数据
 *
 * @author sxie
 */
@TableName("ai_chat_message")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessagePO {

    /**
     * 主键ID
     *
     * 用于持久化唯一标识
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 会话 ID
     *
     * 标识消息所属话
     */
    private Long sessionId;

    /**
     * 消息角色(user/assistant)
     *
     * 区分消息来源
     */
    private String role;

    /**
     * 消息内容
     *
     * 记录对话内容
     */
    private String content;

    /**
     * 实际使用的模型ID
     *
     * 记录调用时使用的模型
     */
    private Long modelId;

    /**
     * 提示词 token 数
     *
     * 用于成本统计与限额控制
     */
    private Integer promptTokens;

    /**
     * 输出 token 数
     *
     * 用于成本统计与限额控制
     */
    private Integer completionTokens;

    /**
     * 总 token 数
     *
     * 便于统一统计
     */
    private Integer totalTokens;

    /**
     * 创建时间
     *
     * 用于时序分析与审计
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
