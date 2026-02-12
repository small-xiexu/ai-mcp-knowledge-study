package com.xbk.knowledge.trigger.gateway.handler.impl;

import com.xbk.knowledge.domain.service.gateway.GatewayToolService;
import com.xbk.knowledge.trigger.gateway.handler.IRequestHandler;
import com.xbk.knowledge.trigger.gateway.model.McpSchemaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * tools/call 处理器
 *
 * @author xiexu
 */
@Slf4j
@Service("gatewayToolsCallHandler")
@RequiredArgsConstructor
public class GatewayToolsCallHandler implements IRequestHandler {

    private final GatewayToolService gatewayToolService;

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
