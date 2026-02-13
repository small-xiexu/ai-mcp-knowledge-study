package com.xbk.knowledge.trigger.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
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
     * 为什么：配置数量可能增长，分页保证接口稳定
     * 入参：分页查询请求
     * 出参：分页结果
     */
    @PostMapping("/list")
    @SaCheckPermission("tool:read")
    public Result<PageResult<McpServerConfigResponse>> listConfigs(@Valid @RequestBody McpServerConfigQueryRequest request) {
        int offset = request.getOffset();
        Integer pageSize = request.getPageSize();
        McpServerConfigPageQuery query = new McpServerConfigPageQuery(offset, pageSize);
        PageResult<McpServerConfig> pageResult = mcpServerConfigAppService.queryMcpServerConfigPage(query);

        /*
         * 目的：统一分页转换逻辑，保障响应格式一致
         */
        PageResult<McpServerConfigResponse> result = PageResultConverter.convert(pageResult, this::convertToResponse);

        return Result.success(result);
    }

    /**
     * 根据 ID 查询 MCP Server 配置
     *
     * 为什么：前端进入配置详情时只关心单条记录
     * 入参：ID 查询请求
     * 出参：MCP Server 配置
     */
    @PostMapping("/get")
    @SaCheckPermission("tool:read")
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
     * 为什么：统一由应用层处理校验与持久化
     * 入参：MCP Server 配置请求
     * 出参：创建后的配置
     */
    @PostMapping("/create")
    @SaCheckPermission("tool:write")
    public Result<McpServerConfigResponse> createConfig(@Valid @RequestBody McpServerConfigRequest request) {
        /*
         * 目的：从请求 DTO 组装领域实体，避免接口层结构泄露
         */
        McpServerConfig config = buildFromRequest(request);
        McpServerConfig savedConfig = mcpServerConfigAppService.createMcpServerConfig(config);
        McpServerConfigResponse response = convertToResponse(savedConfig);
        return Result.success("MCP Server 配置创建成功", response);
    }

    /**
     * 更新 MCP Server 配置
     *
     * 为什么：保持配置管理入口统一，便于审计与回溯
     * 入参：MCP Server 配置请求
     * 出参：更新后的配置
     */
    @PostMapping("/update")
    @SaCheckPermission("tool:write")
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
     * 为什么：允许清理无效配置，避免运行时加载失败
     * 入参：ID 查询请求
     * 出参：删除结果
     */
    @PostMapping("/delete")
    @SaCheckPermission("tool:write")
    public Result<Void> deleteConfig(@Valid @RequestBody IdRequest request) {
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        mcpServerConfigAppService.deleteMcpServerConfig(idQuery);
        return Result.success();
    }

    /**
     * 启用 MCP Server
     *
     * 为什么：启用后允许运行时连接与工具调用
     * 入参：ID 查询请求
     * 出参：更新后的配置
     */
    @PostMapping("/enable")
    @SaCheckPermission("tool:write")
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
     * 为什么：禁用后停止运行时连接与工具调用
     * 入参：ID 查询请求
     * 出参：更新后的配置
     */
    @PostMapping("/disable")
    @SaCheckPermission("tool:write")
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
     * 为什么：配置变更后需要触发运行时重建连接
     * 入参：无
     * 出参：操作结果
     */
    @PostMapping("/refresh")
    @SaCheckPermission("tool:write")
    public Result<Void> refreshConfigs() {
        mcpServerConfigAppService.refreshEnabledServers();
        return Result.success("MCP Server 刷新成功", null);
    }

    /**
     * 刷新指定 MCP Server 运行时连接
     *
     * 为什么：单条刷新避免影响其它运行中配置
     * 入参：ID 查询请求
     * 出参：操作结果
     */
    @PostMapping("/refresh-one")
    @SaCheckPermission("tool:write")
    public Result<Void> refreshConfig(@Valid @RequestBody IdRequest request) {
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        mcpServerConfigAppService.refreshServer(idQuery);
        return Result.success("MCP Server 刷新成功", null);
    }

    private McpServerConfig buildFromRequest(McpServerConfigRequest request) {
        /*
         * 目的：统一构建领域对象，保证入参映射可维护
         */
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
        /*
         * 目的：补充运行时状态，前端无需二次调用查询
         */
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
        /*
         * 目的：序列化可变结构字段，避免表结构频繁变更
         * 约束：序列化失败时返回 null，交由应用层处理
         */
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
        /*
         * 目的：将存储的 JSON 数组解析为列表，给前端可直接展示
         * 约束：解析失败时降级为空列表
         */
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
        /*
         * 目的：将存储的 JSON 对象解析为 Map，确保前端表单可直接回显
         * 约束：解析失败时降级为空 Map
         */
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("解析 MCP map 失败，json: {}", json, e);
            return Collections.emptyMap();
        }
    }
}
