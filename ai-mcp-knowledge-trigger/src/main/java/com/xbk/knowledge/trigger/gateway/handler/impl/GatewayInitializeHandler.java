package com.xbk.knowledge.trigger.gateway.handler.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xbk.knowledge.domain.service.gateway.GatewayToolService;
import com.xbk.knowledge.trigger.gateway.handler.IRequestHandler;
import com.xbk.knowledge.trigger.gateway.model.McpSchemaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * initialize 处理器
 *
 * @author xiexu
 */
@Slf4j
@Service("gatewayInitializeHandler")
@RequiredArgsConstructor
public class GatewayInitializeHandler implements IRequestHandler {

    private final GatewayToolService gatewayToolService;

    @Override
    public McpSchemaVO.JSONRPCResponse handle(String gatewayId, McpSchemaVO.JSONRPCRequest request) {
        Object requestId = request.getId();
        McpSchemaVO.InitializeRequest initializeRequest = McpSchemaVO.unmarshalFrom(
                request.getParams(),
                new TypeReference<McpSchemaVO.InitializeRequest>() {
                }
        );

        GatewayToolService.GatewayCapability capability = gatewayToolService.initialize(gatewayId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", initializeRequest == null || initializeRequest.getProtocolVersion() == null
                ? capability.protocolVersion()
                : initializeRequest.getProtocolVersion());

        Map<String, Object> caps = new HashMap<>();
        caps.put("completions", new HashMap<>());
        caps.put("logging", new HashMap<>());
        caps.put("prompts", Map.of("listChanged", Boolean.TRUE));
        caps.put("resources", Map.of("listChanged", Boolean.TRUE, "subscribe", Boolean.FALSE));
        caps.put("tools", Map.of("listChanged", Boolean.TRUE));
        result.put("capabilities", caps);

        result.put("serverInfo", Map.of(
                "name", capability.serverName(),
                "version", capability.serverVersion()
        ));
        result.put("instructions", capability.instructions());

        return new McpSchemaVO.JSONRPCResponse(McpSchemaVO.JSONRPC_VERSION, requestId, result, null);
    }
}
