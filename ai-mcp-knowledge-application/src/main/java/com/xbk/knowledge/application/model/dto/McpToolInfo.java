package com.xbk.knowledge.application.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 工具信息
 *
 * 职责：应用层 DTO，用于对外展示工具能力
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolInfo {

    /**
     * 工具名称
     *
     * 为什么：用于唯一标识工具
     */
    private String name;

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
