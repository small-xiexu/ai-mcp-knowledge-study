package com.xbk.knowledge.domain.mcp.model.valobj;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP Server 名称查询条件值对象。
 *
 * @author sxie
 */
@Data
@NoArgsConstructor
public class McpServerNameQuery {

    private String serverName;

    public McpServerNameQuery(String serverName) {
        this.serverName = serverName;
    }
}
