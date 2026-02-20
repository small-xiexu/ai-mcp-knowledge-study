package com.xbk.knowledge.api.dto.mcp;

import lombok.Data;

/**
 * MCP 工具信息响应
 *
 * @author sxie
 */
@Data
public class McpToolResponse {

    /**
     * 工具名称
     */
    private String name;

    /**
     * 平台治理主键（toolKey）。
     */
    private String toolKey;

    /**
     * 工具来源（GATEWAY/MCP）。
     */
    private String source;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 入参 Schema(JSON)
     */
    private String inputSchema;
}
