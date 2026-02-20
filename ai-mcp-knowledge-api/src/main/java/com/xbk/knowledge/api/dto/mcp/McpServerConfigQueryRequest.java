package com.xbk.knowledge.api.dto.mcp;

import com.xbk.knowledge.types.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MCP Server 配置查询请求
 * 用于分页查询 MCP Server 配置列表
 *
 * 职责：接口层 DTO，用于承载请求/响应参数并保证传输边界稳定
 * @author sxie
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class McpServerConfigQueryRequest extends PageRequest {

    private static final long serialVersionUID = 1L;
}
