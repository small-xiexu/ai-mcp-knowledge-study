package com.xbk.knowledge.domain.agent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Agent 实体（平台一等对象）。
 *
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Agent {

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * Agent 对外编码（路由主键）。
     */
    private String agentCode;

    /**
     * Agent 名称。
     */
    private String agentName;

    /**
     * Agent 描述。
     */
    private String description;

    /**
     * 调用通道agent/chat_stream。
     */
    private String channel;

    /**
     * ENABLED/DISABLED。
     */
    private String status;

    /**
     * 当前发布版本ID。
     */
    private Long currentPublishedVersionId;

    /**
     * 创建人 ID。
     */
    private Long createdBy;

    /**
     * 更新人 ID。
     */
    private Long updatedBy;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
