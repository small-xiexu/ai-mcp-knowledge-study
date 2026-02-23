package com.xbk.knowledge.domain.agentenhancer.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AgentEnhancer 资产实体。
 *
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentEnhancer {
    private Long id;

    /**
     * 对外唯一编码。
     */
    private String agentEnhancerCode;

    private String agentEnhancerName;

    /**
     * AgentEnhancer 类型（用于运行时装配）。
     *
     * 建议值：
     * - CHAT_MEMORY
     * - REQUEST_RESPONSE_LOG
     * - TOOL_CALL_LOG
     */
    private String agentEnhancerType;

    /**
     * 是否启用（1启用 0禁用）。
     */
    private Integer enabled;

    /**
     * 类型配置（JSON 字符串）。
     */
    private String configJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
