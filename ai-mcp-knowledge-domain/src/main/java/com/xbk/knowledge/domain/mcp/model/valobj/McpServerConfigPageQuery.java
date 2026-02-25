package com.xbk.knowledge.domain.mcp.model.valobj;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP Server 配置分页查询条件值对象。
 *
 * @author sxie
 */
@Data
@NoArgsConstructor
public class McpServerConfigPageQuery {

    /**
     * 分页偏移量。
     */
    private Integer offset;

    /**
     * 分页大小。
     */
    private Integer pageSize;

    public McpServerConfigPageQuery(Integer offset, Integer pageSize) {
        this.offset = offset;
        this.pageSize = pageSize;
    }
}
