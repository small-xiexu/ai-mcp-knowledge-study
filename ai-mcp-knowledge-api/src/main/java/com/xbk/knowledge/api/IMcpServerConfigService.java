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
     * @param request 请求参数
     * @return 列表结果
     */
    Result<PageResult<McpServerConfigResponse>> listConfigs(McpServerConfigQueryRequest request);

    /**
     * 查询配置详情。
     *
     * @param request 请求参数
     * @return 查询结果
     */
    Result<McpServerConfigResponse> getConfig(IdRequest request);

    /**
     * 创建配置。
     *
     * @param request 请求参数
     * @return 创建结果
     */
    Result<McpServerConfigResponse> createConfig(McpServerConfigRequest request);

    /**
     * 更新配置。
     *
     * @param request 请求参数
     * @return 更新结果
     */
    Result<McpServerConfigResponse> updateConfig(McpServerConfigRequest request);

    /**
     * 删除配置。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<Void> deleteConfig(IdRequest request);

    /**
     * 启用配置。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<McpServerConfigResponse> enableConfig(IdRequest request);

    /**
     * 禁用配置。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<McpServerConfigResponse> disableConfig(IdRequest request);

    /**
     * 刷新全部配置。
     *
     * @return 处理结果
     */
    Result<Void> refreshConfigs();

    /**
     * 刷新单个配置。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<Void> refreshConfig(IdRequest request);
}
