package com.xbk.knowledge.domain.chat.model.entity;

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
 * 聊天会话实体
 * 对应数据库表：ai_chat_session
 *
 * 职责：领域实体，用于承载会话状态与生命周期
 *
 * @author sxie
 */
@TableName("ai_chat_session")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    /**
     * 主键ID
     *
     * 为什么：用于持久化唯一标识
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * scopeId。
     */

    /**
     * 会话归属用户ID。
     *
     * 为什么：部门内仍需要区分用户归属，便于权限与清理策略。
     */
    private Long ownerUserId;

    /**
     * 会话标题
     *
     * 为什么：前端展示与检索需要标题
     */
    private String title;

    /**
     * 会话默认模型ID
     *
     * 为什么：记录会话默认模型，便于复现上下文
     */
    private Long modelId;

    /**
     * 关联知识库标签(JSON文本)
     *
     * 为什么：支持按标签检索与上下文增强
     */
    private String ragTags;

    /**
     * Agent ID（多 Agent 平台绑定）。
     */
    private Long agentId;

    /**
     * AgentVersion ID（多 Agent 平台绑定）。
     */
    private Long agentVersionId;

    /**
     * 创建时间
     *
     * 为什么：用于时序分析与审计
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     *
     * 为什么：用于判断会话活跃度与清理
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
