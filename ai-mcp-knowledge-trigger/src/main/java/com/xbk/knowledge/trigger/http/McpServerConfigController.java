package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.IMcpServerConfigService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.api.dto.mcp.McpServerConfigQueryRequest;
import com.xbk.knowledge.api.dto.mcp.McpServerConfigRequest;
import com.xbk.knowledge.api.dto.mcp.McpServerConfigResponse;
import com.xbk.knowledge.application.service.app.McpServerConfigAppService;
import com.xbk.knowledge.application.service.runtime.McpServerRuntimeService;
import com.xbk.knowledge.domain.mcp.model.entity.McpServerConfig;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.mcp.model.valobj.McpServerConfigPageQuery;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageResultConverter;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.types.json.JsonMapUtils;
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
 * @author sxie
 */
@Slf4j
@RestController
@RequestMapping("/api/mcp/servers")
@RequiredArgsConstructor
public class McpServerConfigController implements IMcpServerConfigService {

    private final McpServerConfigAppService mcpServerConfigAppService;
    private final McpServerRuntimeService mcpServerRuntimeService;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询 MCP Server 配置列表。
     * 流程：
     * 1. 进入接口后执行 `tool:read` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 组装 `McpServerConfigPageQuery` 并调用应用服务分页查询。
     * 4. 将领域分页结果转换为 `McpServerConfigResponse` 分页结构。
     * 5. 统一封装 `Result.success` 返回。
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    @PostMapping("/list")
    @SaCheckPermission("tool:read")
    @Override
    public Result<PageResult<McpServerConfigResponse>> listConfigs(@Valid @RequestBody McpServerConfigQueryRequest request) {
        int offset = request.getOffset();
        Integer pageSize = request.getPageSize();
        McpServerConfigPageQuery query = new McpServerConfigPageQuery(offset, pageSize);
        PageResult<McpServerConfig> pageResult = mcpServerConfigAppService.queryMcpServerConfigPage(query);

        // 统一分页转换逻辑，保障响应格式一致
        PageResult<McpServerConfigResponse> result = PageResultConverter.convert(pageResult, this::convertToResponse);

        return Result.success(result);
    }

    /**
     * 根据 ID 查询 MCP Server 配置详情。
     * 流程：
     * 1. 进入接口后执行 `tool:read` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 组装 `IdQuery` 并调用应用服务查询实体。
     * 4. 转换为 `McpServerConfigResponse`（包含运行状态）。
     * 5. 统一封装 `Result.success` 返回。
     *
     * @param request ID 查询请求
     * @return MCP Server 配置
     */
    @PostMapping("/get")
    @SaCheckPermission("tool:read")
    @Override
    public Result<McpServerConfigResponse> getConfig(@Valid @RequestBody IdRequest request) {
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        McpServerConfig config = mcpServerConfigAppService.queryMcpServerConfigById(idQuery);
        McpServerConfigResponse response = convertToResponse(config);
        return Result.success(response);
    }

    /**
     * 创建 MCP Server 配置。
     * 流程：
     * 1. 进入接口后执行 `tool:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 将请求 DTO 转换为 `McpServerConfig` 领域对象。
     * 4. 调用 `mcpServerConfigAppService.createMcpServerConfig` 执行创建。
     * 5. 转换响应并返回“创建成功”结果。
     *
     * @param request MCP Server 配置请求
     * @return 创建后的配置
     */
    @PostMapping("/create")
    @SaCheckPermission("tool:write")
    @Override
    public Result<McpServerConfigResponse> createConfig(@Valid @RequestBody McpServerConfigRequest request) {
        // 从请求 DTO 组装领域实体，避免接口层结构泄露
        McpServerConfig config = buildFromRequest(request);
        McpServerConfig savedConfig = mcpServerConfigAppService.createMcpServerConfig(config);
        McpServerConfigResponse response = convertToResponse(savedConfig);
        return Result.success("MCP Server 配置创建成功", response);
    }

    /**
     * 更新 MCP Server 配置。
     * 流程：
     * 1. 进入接口后执行 `tool:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 组装领域对象并补齐待更新 id。
     * 4. 调用 `mcpServerConfigAppService.updateMcpServerConfig` 执行更新。
     * 5. 转换响应并返回“更新成功”结果。
     *
     * @param request MCP Server 配置请求
     * @return 更新后的配置
     */
    @PostMapping("/update")
    @SaCheckPermission("tool:write")
    @Override
    public Result<McpServerConfigResponse> updateConfig(@Valid @RequestBody McpServerConfigRequest request) {
        McpServerConfig config = buildFromRequest(request);
        config.setId(request.getId());
        McpServerConfig savedConfig = mcpServerConfigAppService.updateMcpServerConfig(config);
        McpServerConfigResponse response = convertToResponse(savedConfig);
        return Result.success("MCP Server 配置更新成功", response);
    }

    /**
     * 删除 MCP Server 配置。
     * 流程：
     * 1. 进入接口后执行 `tool:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 组装 `IdQuery` 并调用应用服务删除。
     * 4. 应用层执行引用校验与删除逻辑。
     * 5. 统一封装空成功结果返回。
     *
     * @param request ID 查询请求
     * @return 删除结果
     */
    @PostMapping("/delete")
    @SaCheckPermission("tool:write")
    @Override
    public Result<Void> deleteConfig(@Valid @RequestBody IdRequest request) {
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        mcpServerConfigAppService.deleteMcpServerConfig(idQuery);
        return Result.success();
    }

    /**
     * 启用 MCP Server。
     * 流程：
     * 1. 进入接口后执行 `tool:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 组装 `IdQuery` 并调用应用服务执行启用。
     * 4. 将启用后的实体转换为响应 DTO。
     * 5. 返回“启用成功”的统一结果。
     *
     * @param request ID 查询请求
     * @return 更新后的配置
     */
    @PostMapping("/enable")
    @SaCheckPermission("tool:write")
    @Override
    public Result<McpServerConfigResponse> enableConfig(@Valid @RequestBody IdRequest request) {
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        McpServerConfig savedConfig = mcpServerConfigAppService.enableMcpServer(idQuery);
        McpServerConfigResponse response = convertToResponse(savedConfig);
        return Result.success("MCP Server 启用成功", response);
    }

    /**
     * 禁用 MCP Server。
     * 流程：
     * 1. 进入接口后执行 `tool:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 组装 `IdQuery` 并调用应用服务执行禁用。
     * 4. 将禁用后的实体转换为响应 DTO。
     * 5. 返回“禁用成功”的统一结果。
     *
     * @param request ID 查询请求
     * @return 更新后的配置
     */
    @PostMapping("/disable")
    @SaCheckPermission("tool:write")
    @Override
    public Result<McpServerConfigResponse> disableConfig(@Valid @RequestBody IdRequest request) {
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        McpServerConfig savedConfig = mcpServerConfigAppService.disableMcpServer(idQuery);
        McpServerConfigResponse response = convertToResponse(savedConfig);
        return Result.success("MCP Server 禁用成功", response);
    }

    /**
     * 刷新所有已启用 MCP Server 的运行时连接。
     * 流程：
     * 1. 进入接口后执行 `tool:write` 权限校验。
     * 2. Controller 调用 `mcpServerConfigAppService.refreshEnabledServers`。
     * 3. 应用层重建运行时连接并同步工具目录。
     * 4. Controller 不返回业务数据，仅返回执行状态。
     * 5. 返回“刷新成功”的统一结果。
     *
     * @return 操作结果
     */
    @PostMapping("/refresh")
    @SaCheckPermission("tool:write")
    @Override
    public Result<Void> refreshConfigs() {
        mcpServerConfigAppService.refreshEnabledServers();
        return Result.success("MCP Server 刷新成功", null);
    }

    /**
     * 刷新指定 MCP Server 的运行时连接。
     * 流程：
     * 1. 进入接口后执行 `tool:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 组装 `IdQuery` 并调用 `refreshServer`。
     * 4. 应用层仅重建目标 Server 连接与工具缓存。
     * 5. 返回“刷新成功”的统一结果。
     *
     * @param request ID 查询请求
     * @return 操作结果
     */
    @PostMapping("/refresh-one")
    @SaCheckPermission("tool:write")
    @Override
    public Result<Void> refreshConfig(@Valid @RequestBody IdRequest request) {
        Long id = request.getId();
        IdQuery idQuery = new IdQuery(id);
        mcpServerConfigAppService.refreshServer(idQuery);
        return Result.success("MCP Server 刷新成功", null);
    }

    private McpServerConfig buildFromRequest(McpServerConfigRequest request) {
        // 统一构建领域对象，保证入参映射可维护
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

    /**
     * 将领域对象转换为响应。
     *
     * @param config 配置对象。
     * @return 返回McpServerConfigResponse对象。
     */
    private McpServerConfigResponse convertToResponse(McpServerConfig config) {
        if (config == null) {
            return null;
        }
        // 补充运行时状态，前端无需二次调用查询
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

    /**
     * 将对象序列化为JSON 字符串。
     *
     * @param value 输入值。
     * @return 返回 JSON 字符串。
     */
    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        // 序列化可变结构字段，避免表结构频繁变更
         // 约束：序列化失败时返回 null，交由应用层处理
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("序列化 MCP 配置失败: {}", value, e);
            return null;
        }
    }

    /**
     * 解析字符串列表。
     *
     * @param json JSON 字符串。
     * @return 返回解析后的列表结果。
     */
    private List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        // 将存储的 JSON 数组解析为列表，给前端可直接展示
         // 约束：解析失败时降级为空列表
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析 MCP args 失败，json: {}", json, e);
            return Collections.emptyList();
        }
    }

    /**
     * 解析字符串映射。
     *
     * @param json JSON 字符串。
     * @return 返回字符串映射。
     */
    private Map<String, String> parseStringMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        // 将存储的 JSON 对象解析为 Map，确保前端表单可直接回显
         // 约束：解析失败时降级为空 Map
        try {
            return JsonMapUtils.readStringMap(objectMapper, json);
        } catch (Exception e) {
            log.warn("解析 MCP map 失败，json: {}", json, e);
            return Collections.emptyMap();
        }
    }
}
