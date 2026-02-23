package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.IMcpToolService;
import cn.dev33.satoken.annotation.SaCheckPermission;
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
 * @author sxie
 */
@RestController
@RequestMapping("/api/mcp/tools")
@RequiredArgsConstructor
public class McpToolController implements IMcpToolService {

    private final McpToolCatalogService mcpToolCatalogService;

    /**
     * 查询可用工具列表。
     * 流程：
     * 1. 进入接口后先执行 `tool:read` 权限校验。
     * 2. Controller 调用 `mcpToolCatalogService.listTools` 拉取工具目录。
     * 3. 将应用层 `McpToolInfo` 列表映射为对外 `McpToolResponse` 列表。
     * 4. 统一通过 `Result.success` 返回给前端配置页使用。
     *
     * @return 工具列表
     */
    @PostMapping("/list")
    @SaCheckPermission("tool:read")
    @Override
    public Result<List<McpToolResponse>> listTools() {
        List<McpToolInfo> tools = mcpToolCatalogService.listTools();
        // 输出层只暴露必要字段，避免内部结构外泄
        List<McpToolResponse> responses = tools.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return Result.success(responses);
    }

    private McpToolResponse toResponse(McpToolInfo info) {
        if (info == null) {
            return null;
        }
        // 统一 DTO 映射入口，便于后续字段扩展
        McpToolResponse response = new McpToolResponse();
        response.setName(info.getName());
        response.setToolKey(info.getToolKey());
        response.setSource(info.getSource());
        response.setDescription(info.getDescription());
        response.setInputSchema(info.getInputSchema());
        return response;
    }
}
