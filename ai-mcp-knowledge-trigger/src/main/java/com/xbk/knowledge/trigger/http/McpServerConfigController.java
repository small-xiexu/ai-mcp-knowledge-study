package com.xbk.knowledge.trigger.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.api.dto.mcp.McpServerConfigQueryRequest;
import com.xbk.knowledge.api.dto.mcp.McpServerConfigRequest;
import com.xbk.knowledge.api.dto.mcp.McpServerConfigResponse;
import com.xbk.knowledge.application.service.app.McpServerConfigAppService;
import com.xbk.knowledge.application.service.runtime.McpServerRuntimeService;
import com.xbk.knowledge.domain.model.entity.McpServerConfig;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.mcp.McpServerConfigPageQuery;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageResultConverter;
import com.xbk.knowledge.types.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * MCP Server 配置管理 Controller
 * 负责接收 HTTP 请求，调用应用服务，转换响应
 *
 * 职责：HTTP 接口适配，用于转发应用层能力
 * @author xiexu
 */
@Slf4j
@RestController
@RequestMapping("/api/mcp/servers")
@RequiredArgsConstructor
public class McpServerConfigController {

    private final McpServerConfigAppService mcpServerConfigAppService;
    private final McpServerRuntimeService mcpServerRuntimeService;
    private final ObjectMapper objectMapper;

    /**
     * 查询 MCP Server 配置列表（分页）
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    @PostMapping("/list")
    public Result<PageResult<McpServerConfigResponse>> listConfigs(@Valid @RequestBody McpServerConfigQueryRequest request) {
        int offset = request.getOffset();
        Integer pageSize = request.getPageSize();
        McpServerConfigPageQuery query = new McpServerConfigPageQuery(offset, pageSize);
        PageResult<McpServerConfig> pageResult = mcpServerConfigAppService.queryMcpServerConfigPage(query);

        PageResult<McpServerConfigResponse> result = PageResultConverter.convert(pageResult, this::convertToResponse);

        return Result.success(result);
    }

    /**
     * 根据 ID 查询 MCP Server 配置
     *
     * @param request ID 查询请求
     * @return MCP Server 配置
     */
    @PostMapping("/get")
    public Result<McpServerConfigResponse> getConfig(@Valid @RequestBody IdRequest request) {
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        McpServerConfig config = mcpServerConfigAppService.queryMcpServerConfigById(idQuery);
        McpServerConfigResponse response = convertToResponse(config);
        return Result.success(response);
    }

    /**
     * 创建 MCP Server 配置
     *
     * @param request MCP Server 配置请求
     * @return 创建后的配置
     */
    @PostMapping("/create")
    public Result<McpServerConfigResponse> createConfig(@Valid @RequestBody McpServerConfigRequest request) {
        McpServerConfig config = buildFromRequest(request);
        McpServerConfig savedConfig = mcpServerConfigAppService.createMcpServerConfig(config);
        McpServerConfigResponse response = convertToResponse(savedConfig);
        return Result.success("MCP Server 配置创建成功", response);
    }

    /**
     * 更新 MCP Server 配置
     *
     * @param request MCP Server 配置请求
     * @return 更新后的配置
     */
    @PostMapping("/update")
    public Result<McpServerConfigResponse> updateConfig(@Valid @RequestBody McpServerConfigRequest request) {
        McpServerConfig config = buildFromRequest(request);
        config.setId(request.getId());
        McpServerConfig savedConfig = mcpServerConfigAppService.updateMcpServerConfig(config);
        McpServerConfigResponse response = convertToResponse(savedConfig);
        return Result.success("MCP Server 配置更新成功", response);
    }

    /**
     * 删除 MCP Server 配置
     *
     * @param request ID 查询请求
     * @return 删除结果
     */
    @PostMapping("/delete")
    public Result<Void> deleteConfig(@Valid @RequestBody IdRequest request) {
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        mcpServerConfigAppService.deleteMcpServerConfig(idQuery);
        return Result.success();
    }

    /**
     * 启用 MCP Server
     *
     * @param request ID 查询请求
     * @return 更新后的配置
     */
    @PostMapping("/enable")
    public Result<McpServerConfigResponse> enableConfig(@Valid @RequestBody IdRequest request) {
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        McpServerConfig savedConfig = mcpServerConfigAppService.enableMcpServer(idQuery);
        McpServerConfigResponse response = convertToResponse(savedConfig);
        return Result.success("MCP Server 启用成功", response);
    }

    /**
     * 禁用 MCP Server
     *
     * @param request ID 查询请求
     * @return 更新后的配置
     */
    @PostMapping("/disable")
    public Result<McpServerConfigResponse> disableConfig(@Valid @RequestBody IdRequest request) {
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        McpServerConfig savedConfig = mcpServerConfigAppService.disableMcpServer(idQuery);
        McpServerConfigResponse response = convertToResponse(savedConfig);
        return Result.success("MCP Server 禁用成功", response);
    }

    /**
     * 刷新启用的 MCP Server 运行时连接
     *
     * @return 操作结果
     */
    @PostMapping("/refresh")
    public Result<Void> refreshConfigs() {
        mcpServerConfigAppService.refreshEnabledServers();
        return Result.success("MCP Server 刷新成功", null);
    }

    private McpServerConfig buildFromRequest(McpServerConfigRequest request) {
        return McpServerConfig
                .builder()
                .serverName(request.getServerName())
                .serverType(request.getServerType())
                .enabled(request.getEnabled())
                .description(request.getDescription())
                .command(request.getCommand())
                .args(toJson(request.getArgs()))
                .env(toJson(request.getEnv()))
                .endpoint(request.getEndpoint())
                .sseEndpoint(request.getSseEndpoint())
                .headers(toJson(request.getHeaders()))
                .connectTimeoutMs(request.getConnectTimeoutMs())
                .requestTimeoutMs(request.getRequestTimeoutMs())
                .initTimeoutMs(request.getInitTimeoutMs())
                .build();
    }

    private McpServerConfigResponse convertToResponse(McpServerConfig config) {
        if (config == null) {
            return null;
        }
        Long id = config.getId();
        Boolean running = mcpServerRuntimeService.isRunning(id);
        return McpServerConfigResponse
                .builder()
                .id(id)
                .serverName(config.getServerName())
                .serverType(config.getServerType())
                .enabled(config.getEnabled())
                .description(config.getDescription())
                .command(config.getCommand())
                .args(parseStringList(config.getArgs()))
                .env(parseStringMap(config.getEnv()))
                .endpoint(config.getEndpoint())
                .sseEndpoint(config.getSseEndpoint())
                .headers(parseStringMap(config.getHeaders()))
                .connectTimeoutMs(config.getConnectTimeoutMs())
                .requestTimeoutMs(config.getRequestTimeoutMs())
                .initTimeoutMs(config.getInitTimeoutMs())
                .running(running)
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("序列化 MCP 配置失败: {}", value, e);
            return null;
        }
    }

    private List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析 MCP args 失败，json: {}", json, e);
            return Collections.emptyList();
        }
    }

    private Map<String, String> parseStringMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("解析 MCP map 失败，json: {}", json, e);
            return Collections.emptyMap();
        }
    }
}
