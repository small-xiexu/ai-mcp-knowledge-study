package com.xbk.knowledge.api.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent 响应 DTO。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * Agent 编码
     */
    private String agentCode;

    /**
     * Agent 名称
     */
    private String agentName;

    /**
     * 描述
     */
    private String description;

    /**
     * channel
     */
    private String channel;

    /**
     * 状态
     */
    private String status;

    /**
     * 当前已发布版本 ID
     */
    private Long currentPublishedVersionId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
