package com.xbk.knowledge.domain.model.vo.mcp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP Server 名称查询条件值对象
 * 统一承载名称唯一性校验条件
 *
 * 职责：领域值对象，用于表达查询条件语义
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpServerNameQuery {

    /**
     * MCP Server 名称
     */
    private String serverName;
}
