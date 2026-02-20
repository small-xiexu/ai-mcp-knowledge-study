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
    private Long id;

    /**
     * scopeId。
     */

    /**
     * Agent 对外编码（路由主键）。
     */
    private String agentCode;

    private String agentName;

    private String description;

    /**
     * ENABLED/DISABLED。
     */
    private String status;

    /**
     * 当前发布版本ID。
     */
    private Long currentPublishedVersionId;

    private Long createdBy;

    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

