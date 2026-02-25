package com.xbk.knowledge.trigger.gateway.handler.impl;

import com.xbk.knowledge.domain.gateway.service.GatewayToolService;
import com.xbk.knowledge.trigger.gateway.handler.IRequestHandler;
import com.xbk.knowledge.trigger.gateway.model.McpSchemaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP 协议 tools/call 处理器
 *
 * 职责：处理客户端的工具调用请求，解析 name 和 arguments 参数，
 * 委托 GatewayToolService 执行实际的 HTTP 工具调用，将结果封装为 MCP 协议响应
 *
 * @author sxie
 */
@Slf4j
@Service("gatewayToolsCallHandler")
@RequiredArgsConstructor
public class GatewayToolsCallHandler implements IRequestHandler {

    /**
     * Gateway 工具服务。
     */
    private final GatewayToolService gatewayToolService;

    /**
     * 处理业务请求。
     * 
     * @param gatewayId 网关 ID
     * @param request tools/call 协议请求参数。
     * @return McpSchemaVO.JSONRPCResponse 数据。
     */
    @Override
    public McpSchemaVO.JSONRPCResponse handle(String gatewayId, McpSchemaVO.JSONRPCRequest request) {
        Object requestId = request.getId();
        if (!(request.getParams() instanceof Map<?, ?> map)) {
            return invalidParams(requestId, "请求参数必须是 JSON 对象");
        }

        String toolName = stringValue(map.get("name"));
        if (toolName == null || toolName.isBlank()) {
            return invalidParams(requestId, "缺少工具名称 name");
        }

        Map<String, Object> arguments;
        Object argumentsObj = map.get("arguments");
        if (argumentsObj == null) {
            arguments = Collections.emptyMap();
        } else if (argumentsObj instanceof Map<?, ?> rawArgs) {
            arguments = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : rawArgs.entrySet()) {
                if (entry.getKey() instanceof String key && !key.isBlank()) {
                    arguments.put(key, entry.getValue());
                }
            }
        } else {
            return invalidParams(requestId, "arguments 必须是 JSON 对象");
        }

        GatewayToolService.ToolCallResult callResult = gatewayToolService.callTool(gatewayId, toolName, arguments);
        if (!callResult.success()) {
            return new McpSchemaVO.JSONRPCResponse(
                    McpSchemaVO.JSONRPC_VERSION,
                    requestId,
                    null,
                    new McpSchemaVO.JSONRPCResponse.JSONRPCError(-32603, callResult.content(), callResult.errorCode())
            );
        }

        Map<String, Object> result = Map.of(
                "content", new Object[]{
                        Map.of("type", "text", "text", callResult.content())
                }
        );
        return new McpSchemaVO.JSONRPCResponse(McpSchemaVO.JSONRPC_VERSION, requestId, result, null);
    }

    /**
     * 构造参数校验失败的 JSON-RPC 错误响应（错误码 -32602）
     * 
     * @param requestId JSON-RPC 请求 ID。
     * @param message 消息内容。
     * @return JSON-RPC 响应。
     */
     private McpSchemaVO.JSONRPCResponse invalidParams(Object requestId, String message) {
        return new McpSchemaVO.JSONRPCResponse(
                McpSchemaVO.JSONRPC_VERSION,
                requestId,
                null,
                new McpSchemaVO.JSONRPCResponse.JSONRPCError(-32602, message, null)
        );
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
