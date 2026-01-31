package com.xbk.knowledge.domain.model.vo.mcp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP Server 配置分页查询条件值对象
 * 统一承载 MCP Server 配置分页条件
 *
 * 职责：领域值对象，用于表达查询条件语义
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpServerConfigPageQuery {

    /**
     * 偏移量
     */
    private Integer offset;

    /**
     * 每页大小
     */
    private Integer pageSize;
}
