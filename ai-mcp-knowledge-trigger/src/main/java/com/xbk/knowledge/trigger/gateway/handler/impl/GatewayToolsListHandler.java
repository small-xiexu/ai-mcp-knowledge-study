package com.xbk.knowledge.trigger.gateway.handler.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.domain.service.gateway.GatewayToolService;
import com.xbk.knowledge.trigger.gateway.handler.IRequestHandler;
import com.xbk.knowledge.trigger.gateway.model.McpSchemaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * tools/list 处理器
 *
 * @author xiexu
 */
@Slf4j
@Service("gatewayToolsListHandler")
@RequiredArgsConstructor
public class GatewayToolsListHandler implements IRequestHandler {

    private final GatewayToolService gatewayToolService;
    private final ObjectMapper objectMapper;

    @Override
    public McpSchemaVO.JSONRPCResponse handle(String gatewayId, McpSchemaVO.JSONRPCRequest request) {
        List<GatewayToolService.ToolDefinition> definitions = gatewayToolService.listTools(gatewayId);
        List<Map<String, Object>> tools = new ArrayList<>();
        for (GatewayToolService.ToolDefinition definition : definitions) {
            Map<String, Object> tool = new LinkedHashMap<>();
            tool.put("name", definition.name());
            tool.put("description", definition.description());
            tool.put("inputSchema", definition.inputSchema());
            tools.add(tool);
        }

        Map<String, Object> result = Map.of("tools", tools);
        return new McpSchemaVO.JSONRPCResponse(McpSchemaVO.JSONRPC_VERSION, request.getId(), result, null);
    }
}
