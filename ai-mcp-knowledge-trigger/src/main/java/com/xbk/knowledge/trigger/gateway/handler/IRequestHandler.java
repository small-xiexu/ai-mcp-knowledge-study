package com.xbk.knowledge.trigger.gateway.handler;

import com.xbk.knowledge.trigger.gateway.model.McpSchemaVO;

/**
 * Gateway JSON-RPC 请求处理器
 *
 * @author xiexu
 */
public interface IRequestHandler {

    McpSchemaVO.JSONRPCResponse handle(String gatewayId, McpSchemaVO.JSONRPCRequest request);
}
