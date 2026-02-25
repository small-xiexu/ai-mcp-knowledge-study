package com.xbk.knowledge.application.model.preheat;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 预热结果（用于控制面展示）。
 *
 * @author sxie
 */
@Getter
@Builder
public class PreheatResult {

    /**
     * 预热目标类型。
     */
    private final String targetType;

    /**
     * 预热目标 ID。
     */
    private final Long targetId;

    /**
     * 是否执行了 MCP 刷新。
     */
    private final boolean mcpRefreshed;

    /**
     * 工具回调是否预热成功。
     */
    private final boolean toolCallbacksWarmed;

    /**
     * Agent 增强器是否预热成功。
     */
    private final boolean agentEnhancersWarmed;

    /**
     * Workflow 是否通过校验。
     */
    private final boolean workflowValidated;

    /**
     * 预热过程警告列表。
     */
    private final List<String> warnings;
}
