package com.xbk.knowledge.trigger.gateway.handler.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xbk.knowledge.domain.gateway.service.GatewayToolService;
import com.xbk.knowledge.trigger.gateway.handler.IRequestHandler;
import com.xbk.knowledge.trigger.gateway.model.McpSchemaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP 协议 initialize 握手处理器
 *
 * 职责：处理客户端发起的 initialize 请求，返回服务端能力声明（capabilities）、
 * 协议版本、服务器信息和使用说明
 *
 * @author sxie
 */
@Slf4j
@Service("gatewayInitializeHandler")
@RequiredArgsConstructor
public class GatewayInitializeHandler implements IRequestHandler {

    private final GatewayToolService gatewayToolService;

    /**
     * 处理业务请求。
     *
     * @param gatewayId 网关 ID
     * @param request initialize 协议请求参数。
     * @return 返回 McpSchemaVO.JSONRPCResponse 数据。
     */
    @Override
    public McpSchemaVO.JSONRPCResponse handle(String gatewayId, McpSchemaVO.JSONRPCRequest request) {
        Object requestId = request.getId();
        McpSchemaVO.InitializeRequest initializeRequest = McpSchemaVO.unmarshalFrom(
                request.getParams(),
                new TypeReference<McpSchemaVO.InitializeRequest>() {
                }
        );

        GatewayToolService.GatewayInfo gatewayInfo = gatewayToolService.initialize(gatewayId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", initializeRequest == null || initializeRequest.getProtocolVersion() == null
                ? gatewayInfo.protocolVersion()
                : initializeRequest.getProtocolVersion());

        Map<String, Object> caps = new HashMap<>();
        caps.put("completions", new HashMap<>());
        caps.put("logging", new HashMap<>());
        caps.put("prompts", Map.of("listChanged", Boolean.TRUE));
        caps.put("resources", Map.of("listChanged", Boolean.TRUE, "subscribe", Boolean.FALSE));
        caps.put("tools", Map.of("listChanged", Boolean.TRUE));
        result.put("capabilities", caps);

        result.put("serverInfo", Map.of(
                "name", gatewayInfo.serverName(),
                "version", gatewayInfo.serverVersion()
        ));
        result.put("instructions", gatewayInfo.instructions());

        return new McpSchemaVO.JSONRPCResponse(McpSchemaVO.JSONRPC_VERSION, requestId, result, null);
    }
}
