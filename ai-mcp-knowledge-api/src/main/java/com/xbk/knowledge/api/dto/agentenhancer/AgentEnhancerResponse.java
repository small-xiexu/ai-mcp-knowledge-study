package com.xbk.knowledge.api.dto.agentenhancer;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AgentEnhancer 响应。
 *
 * @author sxie
 */
@Data
public class AgentEnhancerResponse {

    private Long id;
    private String agentEnhancerCode;
    private String agentEnhancerName;
    private String agentEnhancerType;
    private Integer enabled;
    private String configJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
