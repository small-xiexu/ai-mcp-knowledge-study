package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.dto.mcp.McpToolResponse;
import com.xbk.knowledge.application.model.dto.McpToolInfo;
import com.xbk.knowledge.application.service.mcp.McpToolCatalogService;
import com.xbk.knowledge.types.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MCP 工具管理 Controller
 * 提供工具列表查询
 *
 * @author xiexu
 */
@RestController
@RequestMapping("/api/mcp/tools")
@RequiredArgsConstructor
public class McpToolController {

    private final McpToolCatalogService mcpToolCatalogService;

    /**
     * 查询可用工具列表
     *
     * @return 工具列表
     */
    @PostMapping("/list")
    public Result<List<McpToolResponse>> listTools() {
        List<McpToolInfo> tools = mcpToolCatalogService.listTools();
        List<McpToolResponse> responses = tools.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return Result.success(responses);
    }

    private McpToolResponse toResponse(McpToolInfo info) {
        if (info == null) {
            return null;
        }
        McpToolResponse response = new McpToolResponse();
        response.setName(info.getName());
        response.setDescription(info.getDescription());
        response.setInputSchema(info.getInputSchema());
        return response;
    }
}
