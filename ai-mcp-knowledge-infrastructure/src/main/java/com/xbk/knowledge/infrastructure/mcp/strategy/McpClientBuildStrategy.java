package com.xbk.knowledge.infrastructure.mcp.strategy;

import com.xbk.knowledge.domain.mcp.model.entity.McpServerConfig;
import io.modelcontextprotocol.client.McpSyncClient;

/**
 * MCP 客户端构建策略接口
 *
 * 职责：抽象不同协议类型的客户端构建逻辑
 *
 * @author sxie
 */
public interface McpClientBuildStrategy {
    /**
     * 获取当前策略类型。
     *
     * @return 策略类型。
     */
    McpClientBuildStrategyType getType();

    /**
     * 根据配置构建 MCP 同步客户端。
     *
     * @param config MCP 服务配置。
     * @return MCP 同步客户端。
     */
    McpSyncClient build(McpServerConfig config);
}
