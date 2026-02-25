package com.xbk.knowledge.trigger.gateway.handler;

import com.xbk.knowledge.trigger.gateway.model.McpSchemaVO;

/**
 * Gateway JSON-RPC 请求处理器接口
 *
 * 职责：定义 MCP 协议请求的统一处理契约，每种 method（initialize/tools/list/tools/call）
 * 对应一个实现类
 *
 * @author sxie
 */
public interface IRequestHandler {

    /**
     * 处理 JSON-RPC 请求并返回响应
     * 
     * @param gatewayId 网关业务标识
     * @param request JSON-RPC 请求体
     * @return JSON-RPC 响应
     */
    McpSchemaVO.JSONRPCResponse handle(String gatewayId, McpSchemaVO.JSONRPCRequest request);
}
