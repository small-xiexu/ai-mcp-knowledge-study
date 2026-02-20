package com.xbk.knowledge.application.service.runtime;

import com.xbk.knowledge.domain.mcp.model.entity.McpServerConfig;

import java.util.List;

/**
 * MCP Server 运行时管理接口
 * 负责运行时注册、卸载与刷新
 *
 * 职责：应用层运行时接口，用于隔离具体实现
 * @author sxie
 */
public interface McpServerRuntimeService {

    /**
     * 注册或更新 MCP Server 连接
     *
     * @param config MCP Server 配置
     */
    void registerOrUpdate(McpServerConfig config);

    /**
     * 取消注册 MCP Server 连接
     *
     * @param id MCP Server ID
     */
    void unregister(Long id);

    /**
     * 刷新所有启用 MCP Server 连接
     *
     * @param configs 启用配置列表
     */
    void refresh(List<McpServerConfig> configs);

    /**
     * 判断 MCP Server 是否处于运行状态
     *
     * @param id MCP Server ID
     * @return 是否运行
     */
    boolean isRunning(Long id);
}
