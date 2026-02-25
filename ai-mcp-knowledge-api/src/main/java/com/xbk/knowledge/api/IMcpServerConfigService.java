package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.api.dto.mcp.McpServerConfigQueryRequest;
import com.xbk.knowledge.api.dto.mcp.McpServerConfigRequest;
import com.xbk.knowledge.api.dto.mcp.McpServerConfigResponse;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;

/**
 * MCP 服务配置服务接口
 * 定义 MCP 服务配置管理的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IMcpServerConfigService {

    /**
     * 分页查询配置列表。
     * 
     * @param request MCP 服务配置分页查询参数。
     * @return McpServerConfigResponse 分页数据。
     */
    Result<PageResult<McpServerConfigResponse>> listConfigs(McpServerConfigQueryRequest request);

    /**
     * 查询配置详情。
     * 
     * @param request MCP 服务配置查询参数。
     * @return McpServerConfigResponse 数据。
     */
    Result<McpServerConfigResponse> getConfig(IdRequest request);

    /**
     * 创建配置。
     * 
     * @param request MCP 服务配置创建参数。
     * @return McpServerConfigResponse 数据。
     */
    Result<McpServerConfigResponse> createConfig(McpServerConfigRequest request);

    /**
     * 更新配置。
     * 
     * @param request MCP 服务配置更新参数。
     * @return McpServerConfigResponse 数据。
     */
    Result<McpServerConfigResponse> updateConfig(McpServerConfigRequest request);

    /**
     * 删除配置。
     * 
     * @param request MCP 服务配置删除参数。
     * @return MCP 服务配置删除状态。
     */
    Result<Void> deleteConfig(IdRequest request);

    /**
     * 启用配置。
     * 
     * @param request MCP 服务配置启停参数。
     * @return McpServerConfigResponse 数据。
     */
    Result<McpServerConfigResponse> enableConfig(IdRequest request);

    /**
     * 禁用配置。
     * 
     * @param request MCP 服务配置启停参数。
     * @return McpServerConfigResponse 数据。
     */
    Result<McpServerConfigResponse> disableConfig(IdRequest request);

    /**
     * 刷新全部配置。
     * 
     * @return 全量配置刷新状态。
     */
    Result<Void> refreshConfigs();

    /**
     * 刷新单个配置。
     * 
     * @param request MCP 服务配置刷新参数。
     * @return 单个配置刷新状态。
     */
    Result<Void> refreshConfig(IdRequest request);
}
