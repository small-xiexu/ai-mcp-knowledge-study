package com.xbk.knowledge.application.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 工具信息
 *
 * 职责：应用层 DTO，用于对外展示工具能力
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolInfo {

    /**
     * 工具名称
     *
     * 为什么：对外展示的函数名（ToolDefinition.name），也是模型侧实际调用的 tool/function name
     */
    private String name;

    /**
     * 平台治理主键（toolKey）。
     *
     * 说明：用于 allowlist/审计/审批等治理能力，区别于模型侧调用 name。
     */
    private String toolKey;

    /**
     * 工具来源（GATEWAY/MCP）。
     */
    private String source;

    /**
     * 工具描述
     *
     * 为什么：用于提示与展示
     */
    private String description;

    /**
     * 入参 Schema(JSON)
     *
     * 为什么：用于工具参数校验与提示
     */
    private String inputSchema;
}
