package com.xbk.knowledge.domain.chat.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 聊天会话实体
 * 对应数据库表ai_chat_session
 *
 * 职责：领域实体，用于承载会话状态与生命周期
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    /**
     * 主键ID
     *
     * 用于持久化唯一标识
     */
    private Long id;

    /**
     * 话归属用户ID。
     *
     * 部门内仍需要区分用户归属，便于权限与清理策略。
     */
    private Long ownerUserId;

    /**
     * 话标题
     *
     * 前端展示与检索需要标题
     */
    private String title;

    /**
     * 话默认模型ID
     *
     * 记录话默认模型，便于复现上下文
     */
    private Long modelId;

    /**
     * 关联知识库标签(JSON文本)
     *
     * 支持按标签检索与上下文增强
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
     * 用于时序分析与审计
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     *
     * 用于判断话活跃度与清理
     */
    private LocalDateTime updatedAt;
}
