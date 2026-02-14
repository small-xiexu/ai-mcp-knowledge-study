package com.xbk.knowledge.api.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent 响应 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse {

    private Long id;

    private Long orgId;

    private String agentCode;

    private String agentName;

    private String description;

    private String status;

    private Long currentPublishedVersionId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

