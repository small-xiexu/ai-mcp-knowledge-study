package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.mcp.McpToolResponse;
import com.xbk.knowledge.types.common.Result;

import java.util.List;

/**
 * MCP 工具服务接口
 * 定义 MCP 工具列表查询的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IMcpToolService {

    /**
     * 分页查询工具列表。
     *
     * @return 列表结果
     */
    Result<List<McpToolResponse>> listTools();
}
