package com.xbk.knowledge.types.tool;

/**
 * ToolCallback 扩展接口暴露平台治理主键 toolKey。
 *
 * 说明：
 * - LLM 侧使用的是 tool function name（ToolDefinition.name）
 * - 平台治理/审计/审批使用的是 toolKey（稳定、可追溯）
 *
 * @author sxie
 */
public interface ToolKeyAware {

    /**
     * 平台工具唯一键（例如 gateway:{gatewayId}:{toolName} / mcp:{serverName}:{toolName}）。
     * 
     * @return 平台工具唯一键。
     */
    String toolKey();

    /**
     * 工具来源类型（例如 GATEWAY / MCP）。
     * 
     * @return 工具来源类型。
     */
    String toolSource();
}
