package com.xbk.knowledge.api.dto.preheat;

import lombok.Data;

import java.util.List;

/**
 * 预热响应。
 *
 * @author sxie
 */
@Data
public class PreheatResponse {

    /**
     * 目标类型
     */
    private String targetType;
    /**
     * 目标 ID
     */
    private Long targetId;
    /**
     * MCPRefreshed
     */
    private Boolean mcpRefreshed;
    /**
     * 工具CallbacksWarmed
     */
    private Boolean toolCallbacksWarmed;
    /**
     * AgentEnhancersWarmed
     */
    private Boolean agentEnhancersWarmed;
    /**
     * WorkflowValidated
     */
    private Boolean workflowValidated;
    /**
     * warnings
     */
    private List<String> warnings;
}
