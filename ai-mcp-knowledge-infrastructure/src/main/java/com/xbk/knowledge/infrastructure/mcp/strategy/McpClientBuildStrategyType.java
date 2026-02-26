package com.xbk.knowledge.infrastructure.mcp.strategy;

import com.xbk.knowledge.types.enums.McpServerType;

/**
 * MCP 客户端构建策略类型枚举
 *
 * 职责：统一策略工厂内部使用的类型标识
 *
 * @author sxie
 */
public enum McpClientBuildStrategyType {

    /**
     * STDIO 协议构建策略。
     */
    STDIO,

    /**
     * HTTP 协议构建策略。
     */
    HTTP,

    /**
     * SSE 协议构建策略。
     */
    SSE,

    /**
     * WEBSOCKET 协议构建策略。
     */
    WEBSOCKET;

    /**
     * 将 MCP 服务类型转换为策略类型。
     *
     * @param serverType MCP 服务类型。
     * @return 策略类型。
     */
    public static McpClientBuildStrategyType from(McpServerType serverType) {
        if (serverType == null) {
            throw new IllegalArgumentException("MCP Server 类型不能为空");
        }
        try {
            return McpClientBuildStrategyType.valueOf(serverType.name());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("不支持的 MCP Server 类型: " + serverType, e);
        }
    }
}

