package com.xbk.knowledge.application.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 工具信息
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolInfo {

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 入参 Schema(JSON)
     */
    private String inputSchema;
}
