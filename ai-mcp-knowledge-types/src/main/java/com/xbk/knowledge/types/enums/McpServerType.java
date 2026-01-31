package com.xbk.knowledge.types.enums;

/**
 * MCP Server 类型枚举
 * 标识 MCP Server 的接入协议与运行方式
 *
 * 职责：通用枚举，用于统一协议类型语义
 * @author xiexu
 */
public enum McpServerType {

    /**
     * 本地进程标准输入输出模式
     */
    STDIO,

    /**
     * 远程 HTTP（Streamable HTTP）模式
     */
    HTTP,

    /**
     * 远程 SSE（Server-Sent Events）模式
     */
    SSE,

    /**
     * 远程 WebSocket 模式
     */
    WEBSOCKET
}
