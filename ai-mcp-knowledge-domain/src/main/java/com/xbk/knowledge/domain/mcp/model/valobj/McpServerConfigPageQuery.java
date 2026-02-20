package com.xbk.knowledge.domain.mcp.model.valobj;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP Server 配置分页查询条件值对象。
 *
 * @author xiexu
 */
@Data
@NoArgsConstructor
public class McpServerConfigPageQuery {

    private Integer offset;
    private Integer pageSize;

    public McpServerConfigPageQuery(Integer offset, Integer pageSize) {
        this.offset = offset;
        this.pageSize = pageSize;
    }
}
