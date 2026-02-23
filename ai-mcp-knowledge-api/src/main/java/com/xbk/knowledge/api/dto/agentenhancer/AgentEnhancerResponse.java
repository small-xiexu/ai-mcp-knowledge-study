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

    /**
     * 主键ID
     */
    private Long id;
    /**
     * AgentEnhancer编码
     */
    private String agentEnhancerCode;
    /**
     * AgentEnhancer名称
     */
    private String agentEnhancerName;
    /**
     * AgentEnhancer类型
     */
    private String agentEnhancerType;
    /**
     * 启用状态
     */
    private Integer enabled;
    /**
     * config JSON
     */
    private String configJson;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
