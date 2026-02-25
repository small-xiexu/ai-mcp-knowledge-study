package com.xbk.knowledge.trigger.gateway.handler.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.domain.gateway.service.GatewayToolService;
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
 * MCP 协议 tools/list 处理器
 *
 * 职责：处理客户端的工具清单查询请求，从 GatewayToolService 获取已启用的工具定义列表，
 * 转换为 MCP 协议规定的 tools 数组格式返回
 *
 * @author sxie
 */
@Slf4j
@Service("gatewayToolsListHandler")
@RequiredArgsConstructor
public class GatewayToolsListHandler implements IRequestHandler {

    /**
     * Gateway 工具服务。
     */
    private final GatewayToolService gatewayToolService;

    /**
     * JSON 序列化/反序列化组件。
     */
    private final ObjectMapper objectMapper;

    /**
     * 处理业务请求。
     * 
     * @param gatewayId 网关 ID
     * @param request tools/list 协议请求参数。
     * @return McpSchemaVO.JSONRPCResponse 数据。
     */
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
