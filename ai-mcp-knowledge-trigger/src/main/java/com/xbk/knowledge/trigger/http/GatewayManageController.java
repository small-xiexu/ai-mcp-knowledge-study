package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.IGatewayManageService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.api.dto.gateway.GatewayAuthListRequest;
import com.xbk.knowledge.api.dto.gateway.GatewayAuthResponse;
import com.xbk.knowledge.api.dto.gateway.GatewayInstanceRequest;
import com.xbk.knowledge.api.dto.gateway.GatewayInstanceResponse;
import com.xbk.knowledge.api.dto.gateway.GatewayMetricsQueryRequest;
import com.xbk.knowledge.api.dto.gateway.GatewayMetricsResponse;
import com.xbk.knowledge.api.dto.gateway.GatewayModelBindingResponse;
import com.xbk.knowledge.api.dto.gateway.GatewayModelOptionResponse;
import com.xbk.knowledge.api.dto.gateway.GatewayToolDebugResponse;
import com.xbk.knowledge.api.dto.gateway.GatewayToolDetailResponse;
import com.xbk.knowledge.api.dto.gateway.GatewayToolMappingResponse;
import com.xbk.knowledge.api.dto.gateway.GatewayToolOptionResponse;
import com.xbk.knowledge.api.dto.gateway.GatewayToolRefreshResponse;
import com.xbk.knowledge.api.dto.gateway.GatewayToolResponse;
import com.xbk.knowledge.api.dto.gateway.MappingNodeRequest;
import com.xbk.knowledge.api.dto.gateway.ModelBindingQueryRequest;
import com.xbk.knowledge.api.dto.gateway.RefreshToolsRequest;
import com.xbk.knowledge.api.dto.gateway.SaveGatewayAuthRequest;
import com.xbk.knowledge.api.dto.gateway.SaveModelBindingRequest;
import com.xbk.knowledge.api.dto.gateway.SaveToolRequest;
import com.xbk.knowledge.api.dto.gateway.ToolDebugRequest;
import com.xbk.knowledge.api.dto.gateway.ToolListRequest;
import com.xbk.knowledge.application.service.app.GatewayManageAppService;
import com.xbk.knowledge.application.service.app.GatewayObservabilityAppService;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.gateway.model.entity.McpGateway;
import com.xbk.knowledge.domain.gateway.model.entity.McpGatewayAuth;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolBinding;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolMapping;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolRegistry;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.gateway.service.GatewayToolService;
import com.xbk.knowledge.types.common.PageRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.types.enums.GatewayStatus;
import com.xbk.knowledge.trigger.annotation.GatewayToolCall;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Gateway 管理接口
 *
 * @author sxie
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gateway/manage")
public class GatewayManageController implements IGatewayManageService {

    /**
     * 默认网关标识。
     */
    private static final String DEFAULT_GATEWAY_ID = "default_gateway";

    /**
     * 日志链路追踪 ID 的 MDC 键。
     */
    private static final String CALL_ID_MDC_KEY = "gatewayToolCallId";

    /**
     * 工具列表"最近调用"查询窗口（分钟）。
     */
    private static final int TOOL_LIST_RECENT_MINUTES = 24 * 60;

    /**
     * 工具列表"最近调用"时间格式。
     */
    private static final DateTimeFormatter TOOL_LAST_CALL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 网关工具调试服务。
     */
    private final GatewayToolService gatewayToolService;

    /**
     * 网关可观测性应用服务。
     */
    private final GatewayObservabilityAppService gatewayObservabilityAppService;

    /**
     * 网关管理应用服务。
     */
    private final GatewayManageAppService gatewayManageAppService;

    /**
     * 分页查询网关实例列表。
     * 流程：
     * 1. 进入接口后执行 `tool:read` 权限校验。
     * 2. Spring 完成请求体绑定与分页参数校验（`@Valid`）。
     * 3. Controller 委托 `gatewayManageAppService.queryGatewayInstancePage` 查询分页数据。
     * 4. 逐条补充工具数量等展示字段，组装列表行数据。
     * 5. 统一封装 `PageResult` 并返回 `Result.success`。
     *
     * @param request 网关管理分页查询参数。
     */
    @PostMapping("/instances/list")
    @SaCheckPermission("tool:read")
    @Override
    public Result<PageResult<GatewayInstanceResponse>> listGatewayInstances(@Valid @RequestBody PageRequest request) {
        Integer pageNum = request == null ? null : request.getPageNum();
        Integer pageSize = request == null ? null : request.getPageSize();
        PageResult<McpGateway> gatewayPage = gatewayManageAppService.queryGatewayInstancePage(pageNum, pageSize);

        List<GatewayInstanceResponse> records = new ArrayList<>();
        if (!CollectionUtils.isEmpty(gatewayPage.getRecords())) {
            for (McpGateway gateway : gatewayPage.getRecords()) {
                long toolCount = gatewayManageAppService.countToolsByGatewayId(gateway.getGatewayId());
                records.add(toGatewayResponse(gateway, toolCount));
            }
        }
        return Result.success(PageResult.of(records, gatewayPage.getTotal(), gatewayPage.getPageNum(), gatewayPage.getPageSize()));
    }

    /**
     * 创建或更新网关实例。
     * 流程：
     * 1. 进入接口后执行 `tool:write` 权限校验。
     * 2. Spring 绑定请求体，Controller 先校验 `gatewayName` 必填。
     * 3. Controller 委托 `gatewayManageAppService.saveGatewayInstance` 执行保存。
     * 4. Controller 查询工具数量并补齐展示字段。
     * 5. 转换为展示结构并统一封装返回。
     *
     * @param request 网关管理保存参数。
     */
    @PostMapping("/instances/save")
    @SaCheckPermission("tool:write")
    @Override
    public Result<GatewayInstanceResponse> saveGatewayInstance(@RequestBody GatewayInstanceRequest request) {
        if (request == null || !StringUtils.hasText(request.getGatewayName())) {
            throw new IllegalArgumentException("gatewayName 不能为空");
        }
        McpGateway saved = gatewayManageAppService.saveGatewayInstance(
                request.getId(),
                request.getGatewayId(),
                request.getGatewayName(),
                request.getGatewayDesc(),
                request.getGatewayVersion(),
                request.getGatewayInstructions(),
                request.getStatus()
        );
        long toolCount = gatewayManageAppService.countToolsByGatewayId(saved.getGatewayId());
        return Result.success(toGatewayResponse(saved, toolCount));
    }

    /**
     * 删除网关实例。
     * 流程：
     * 1. 进入接口后执行 `tool:write` 权限校验。
     * 2. Spring 绑定请求体并校验 id 是否为空。
     * 3. Controller 调用 `gatewayManageAppService.deleteGatewayInstance`。
     * 4. 应用层执行关联数据清理与删除。
     * 5. 统一封装空成功结果返回。
     *
     * @param request 网关实例删除参数。
     * @return 网关实例删除状态。
     */
    @PostMapping("/instances/delete")
    @SaCheckPermission("tool:write")
    @Override
    public Result<Void> deleteGatewayInstance(@RequestBody IdRequest request) {
        gatewayManageAppService.deleteGatewayInstance(new IdQuery(request == null ? null : request.getId()));
        return Result.success();
    }

    /**
     * 分页查询网关凭证列表。
     * 流程：
     * 1. 进入接口后执行 `tool:read` 权限校验。
     * 2. Spring 绑定请求体，解析 gatewayId 并确保网关存在。
     * 3. 委托应用服务执行凭证过滤与分页。
     * 4. Controller 将领域对象转换为前端展示结构。
     * 5. 统一封装 `PageResult` 返回。
     *
     * @param request 网关管理分页查询参数。
     */
    @PostMapping("/auth/list")
    @SaCheckPermission("tool:read")
    @Override
    public Result<PageResult<GatewayAuthResponse>> listGatewayAuth(@RequestBody GatewayAuthListRequest request) {
        // 1、解析网关并确保网关实例存在。
        String gatewayId = resolveGatewayId(request == null ? null : request.getGatewayId());
        gatewayManageAppService.ensureGatewayExists(gatewayId);

        Integer status = request == null ? null : request.getStatus();
        String apiKeyKeyword = request == null ? null : request.getApiKeyKeyword();
        Integer pageNum = request == null ? null : request.getPageNum();
        Integer pageSize = request == null ? null : request.getPageSize();

        // 2、由应用服务承接过滤与分页逻辑，Controller 仅负责响应结构转换。
        PageResult<McpGatewayAuth> authPage = gatewayManageAppService.queryGatewayAuthPage(
                gatewayId,
                status,
                apiKeyKeyword,
                pageNum,
                pageSize
        );
        List<GatewayAuthResponse> records = new ArrayList<>();
        if (!CollectionUtils.isEmpty(authPage.getRecords())) {
            for (McpGatewayAuth auth : authPage.getRecords()) {
                records.add(toGatewayAuthResponse(auth));
            }
        }
        // 3、统一封装分页结果并返回。
        return Result.success(PageResult.of(records, authPage.getTotal(), authPage.getPageNum(), authPage.getPageSize()));
    }

    /**
     * 创建或更新网关凭证。
     * 流程：
     * 1. 进入接口后执行 `tool:write` 权限校验。
     * 2. Spring 绑定请求体并校验请求对象有效性。
     * 3. Controller 处理 gatewayId 的默认与网关存在性保障。
     * 4. 应用服务执行凭证保存、唯一性校验与状态/限流规则处理。
     * 5. 转换为凭证视图结构并统一封装返回。
     *
     * @param request 网关管理保存参数。
     */
    @PostMapping("/auth/save")
    @SaCheckPermission("tool:write")
    @Override
    public Result<GatewayAuthResponse> saveGatewayAuth(@RequestBody SaveGatewayAuthRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        String gatewayId = null;
        if (request.getId() == null) {
            gatewayId = resolveGatewayId(request.getGatewayId());
            gatewayManageAppService.ensureGatewayExists(gatewayId);
        } else if (StringUtils.hasText(request.getGatewayId())) {
            gatewayId = resolveGatewayId(request.getGatewayId());
            gatewayManageAppService.ensureGatewayExists(gatewayId);
        }
        McpGatewayAuth saved = gatewayManageAppService.saveGatewayAuth(
                request.getId(),
                gatewayId,
                request.getApiKey(),
                request.getRateLimit(),
                request.getExpireTime(),
                request.getStatus()
        );
        return Result.success(toGatewayAuthResponse(saved));
    }

    /**
     * 启用网关凭证。
     * 流程：
     * 1. 进入接口后执行 `tool:write` 权限校验。
     * 2. Spring 绑定请求体并提取 id。
     * 3. Controller 调用统一方法 `executeStatusUpdate(id, GatewayStatus.ENABLED.getCode(), ...)`。
     * 4. 统一方法委托应用服务校验凭证存在并更新状态为启用。
     * 5. 返回统一成功结果。
     *
     * @param request 网关凭证启用参数。
     * @return 网关凭证启用状态。
     */
    @PostMapping("/auth/enable")
    @SaCheckPermission("tool:write")
    @Override
    public Result<Void> enableGatewayAuth(@RequestBody IdRequest request) {
        return executeStatusUpdate(
                request == null ? null : request.getId(),
                GatewayStatus.ENABLED.getCode(),
                gatewayManageAppService::updateGatewayAuthStatus
        );
    }

    /**
     * 禁用网关凭证。
     * 流程：
     * 1. 进入接口后执行 `tool:write` 权限校验。
     * 2. Spring 绑定请求体并提取 id。
     * 3. Controller 调用统一方法 `executeStatusUpdate(id, GatewayStatus.DISABLED.getCode(), ...)`。
     * 4. 统一方法委托应用服务校验凭证存在并更新状态为禁用。
     * 5. 返回统一成功结果。
     *
     * @param request 网关凭证禁用参数。
     * @return 网关凭证禁用状态。
     */
    @PostMapping("/auth/disable")
    @SaCheckPermission("tool:write")
    @Override
    public Result<Void> disableGatewayAuth(@RequestBody IdRequest request) {
        return executeStatusUpdate(
                request == null ? null : request.getId(),
                GatewayStatus.DISABLED.getCode(),
                gatewayManageAppService::updateGatewayAuthStatus
        );
    }

    /**
     * 分页查询网关工具列表（支持关键词搜索和状态筛选）。
     * 流程：
     * 1. 进入接口后执行 `tool:read` 权限校验。
     * 2. Spring 绑定请求体并解析 gatewayId，确保网关存在。
     * 3. 委托应用服务执行工具分页查询与筛选。
     * 4. Controller 聚合可观测性指标并补充最近调用摘要。
     * 5. 逐条转换为前端展示字段并统一封装 `PageResult` 返回。
     *
     * @param request 网关管理分页查询参数（支持 toolNameKeyword/toolDescriptionKeyword/status）。
     */
    @PostMapping("/tools/list")
    @SaCheckPermission("tool:read")
    @Override
    public Result<PageResult<GatewayToolResponse>> listTools(@RequestBody ToolListRequest request) {
        String gatewayId = resolveGatewayId(request == null ? null : request.getGatewayId());
        gatewayManageAppService.ensureGatewayExists(gatewayId);
        Integer pageNum = request == null ? null : request.getPageNum();
        Integer pageSize = request == null ? null : request.getPageSize();

        String toolNameKeyword = request == null ? null : request.getToolNameKeyword();
        String toolDescriptionKeyword = request == null ? null : request.getToolDescriptionKeyword();
        Integer statusFilter = request == null ? null : request.getStatus();

        PageResult<McpToolRegistry> toolPage = gatewayManageAppService.queryToolPage(
                gatewayId,
                toolNameKeyword,
                toolDescriptionKeyword,
                statusFilter,
                pageNum,
                pageSize
        );

        GatewayObservabilityAppService.GatewayMetricsReport metricsReport = gatewayObservabilityAppService.queryMetrics(
                new GatewayObservabilityAppService.MetricsQuery(gatewayId, null, TOOL_LIST_RECENT_MINUTES)
        );
        Map<String, GatewayObservabilityAppService.ToolMetricsSnapshot> latestMetricsByTool = new HashMap<>();
        if (metricsReport != null && !CollectionUtils.isEmpty(metricsReport.toolMetrics())) {
            for (GatewayObservabilityAppService.ToolMetricsSnapshot metric : metricsReport.toolMetrics()) {
                if (metric == null || !StringUtils.hasText(metric.toolName())) {
                    continue;
                }
                latestMetricsByTool.put(metric.toolName(), metric);
            }
        }

        List<GatewayToolResponse> rows = new ArrayList<>();
        if (!CollectionUtils.isEmpty(toolPage.getRecords())) {
            for (McpToolRegistry tool : toolPage.getRecords()) {
                rows.add(toToolResponse(tool, resolveLastCallSummary(latestMetricsByTool.get(tool.getToolName()))));
            }
        }
        return Result.success(PageResult.of(rows, toolPage.getTotal(), toolPage.getPageNum(), toolPage.getPageSize()));
    }

    private String resolveLastCallSummary(GatewayObservabilityAppService.ToolMetricsSnapshot metric) {
        if (metric == null || metric.latestCallAt() == null) {
            return "-";
        }
        return metric.latestCallAt().format(TOOL_LAST_CALL_FORMATTER) + " " + (metric.latestCallSuccess() ? "成功" : "失败");
    }

    /**
     * 查询单个工具详情及映射配置。
     * 流程：
     * 1. 进入接口后执行 `tool:read` 权限校验。
     * 2. Spring 绑定请求体并校验 id 非空。
     * 3. 委托应用服务查询工具主记录与 request/response 映射。
     * 4. Controller 负责将领域对象转换为详情响应结构。
     * 5. 统一封装 `Result.success` 返回。
     *
     * @param request 工具详情查询参数。
     */
    @PostMapping("/tools/get")
    @SaCheckPermission("tool:read")
    @Override
    public Result<GatewayToolDetailResponse> getTool(@RequestBody IdRequest request) {
        Long toolId = request == null ? null : request.getId();
        GatewayManageAppService.ToolDetail toolDetail = gatewayManageAppService.queryToolDetail(toolId);

        List<GatewayToolMappingResponse> requestMappingResponses = new ArrayList<>();
        for (McpToolMapping requestMapping : toolDetail.getRequestMappings()) {
            requestMappingResponses.add(toToolMappingResponse(requestMapping));
        }
        List<GatewayToolMappingResponse> responseMappingResponses = new ArrayList<>();
        for (McpToolMapping responseMapping : toolDetail.getResponseMappings()) {
            responseMappingResponses.add(toToolMappingResponse(responseMapping));
        }

        GatewayToolDetailResponse data = GatewayToolDetailResponse.builder()
                .tool(toToolResponse(toolDetail.getTool(), null))
                .requestMappings(requestMappingResponses)
                .responseMappings(responseMappingResponses)
                .build();
        return Result.success(data);
    }

    /**
     * 创建或更新工具配置。
     * 流程：
     * 1. 进入接口后执行 `tool:write` 权限校验。
     * 2. Spring 绑定请求体并解析 gatewayId，确保网关存在。
     * 3. Controller 将请求体转换为应用服务命令对象。
     * 4. 应用服务执行新增/更新、映射重建与 schema 清理。
     * 5. 返回保存后的工具视图数据。
     *
     * @param request 网关管理保存参数。
     */
    @PostMapping("/tools/save")
    @SaCheckPermission("tool:write")
    @Override
    public Result<GatewayToolResponse> saveTool(@RequestBody SaveToolRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        String gatewayId = resolveGatewayId(request.getGatewayId());
        gatewayManageAppService.ensureGatewayExists(gatewayId);
        GatewayManageAppService.ToolSaveCommand command = GatewayManageAppService.ToolSaveCommand.builder()
                .id(request.getId())
                .gatewayId(gatewayId)
                .toolName(request.getToolName())
                .toolDescription(request.getToolDescription())
                .httpMethod(request.getHttpMethod())
                .httpUrl(request.getHttpUrl())
                .httpHeaders(request.getHttpHeaders())
                .timeout(request.getTimeout())
                .retryTimes(request.getRetryTimes())
                .status(request.getStatus())
                .requestMappings(toToolMappingNodes(request.getRequestMappings()))
                .responseMappings(toToolMappingNodes(request.getResponseMappings()))
                .build();
        McpToolRegistry saved = gatewayManageAppService.saveTool(command);
        return Result.success(toToolResponse(saved, null));
    }

    /**
     * 删除工具配置。
     * 流程：
     * 1. 进入接口后执行 `tool:write` 权限校验。
     * 2. Spring 绑定请求体并校验 id 非空。
     * 3. Controller 调用 `gatewayManageAppService.deleteTool`。
     * 4. 应用层执行工具与关联映射/绑定清理。
     * 5. 统一封装空成功结果返回。
     *
     * @param request 工具删除参数。
     * @return 工具删除状态。
     */
    @PostMapping("/tools/delete")
    @SaCheckPermission("tool:write")
    @Override
    public Result<Void> deleteTool(@RequestBody IdRequest request) {
        gatewayManageAppService.deleteTool(new IdQuery(request == null ? null : request.getId()));
        return Result.success();
    }

    /**
     * 启用工具配置。
     * 流程：
     * 1. 进入接口后执行 `tool:write` 权限校验。
     * 2. Spring 绑定请求体并校验 id 非空。
     * 3. Controller 调用统一方法 `executeStatusUpdate(id, GatewayStatus.ENABLED.getCode(), ...)`。
     * 4. 统一方法委托应用服务校验工具并更新状态为启用。
     * 5. 返回统一成功结果。
     *
     * @param request 工具启用参数。
     * @return 工具启用状态。
     */
    @PostMapping("/tools/enable")
    @SaCheckPermission("tool:write")
    @Override
    public Result<Void> enableTool(@RequestBody IdRequest request) {
        return executeStatusUpdate(
                request == null ? null : request.getId(),
                GatewayStatus.ENABLED.getCode(),
                gatewayManageAppService::updateToolStatus
        );
    }

    /**
     * 禁用工具配置。
     * 流程：
     * 1. 进入接口后执行 `tool:write` 权限校验。
     * 2. Spring 绑定请求体并校验 id 非空。
     * 3. Controller 调用统一方法 `executeStatusUpdate(id, GatewayStatus.DISABLED.getCode(), ...)`。
     * 4. 统一方法委托应用服务校验工具并更新状态为禁用。
     * 5. 返回统一成功结果。
     *
     * @param request 工具禁用参数。
     * @return 工具禁用状态。
     */
    @PostMapping("/tools/disable")
    @SaCheckPermission("tool:write")
    @Override
    public Result<Void> disableTool(@RequestBody IdRequest request) {
        return executeStatusUpdate(
                request == null ? null : request.getId(),
                GatewayStatus.DISABLED.getCode(),
                gatewayManageAppService::updateToolStatus
        );
    }

    /**
     * 调试执行工具并返回调用结果。
     * 流程：
     * 1. 进入接口后执行 `tool:invoke` 权限校验。
     * 2. Spring 绑定请求体并校验 `toolName` 必填。
     * 3. 解析 gatewayId、准备调用参数。
     * 4. 切面自动记录 gatewayToolCallId 和工具调用日志。
     * 5. 调用 `gatewayToolService.callTool` 执行工具。
     * 6. 组装 success/content/errorCode 并统一封装返回。
     *
     * @param request 网关管理调用参数。
     */
    @GatewayToolCall
    @PostMapping("/tools/debug")
    @SaCheckPermission("tool:invoke")
    @Override
    public Result<GatewayToolDebugResponse> debugTool(@RequestBody ToolDebugRequest request) {
        if (request == null || !StringUtils.hasText(request.getToolName())) {
            throw new IllegalArgumentException("toolName 不能为空");
        }
        String gatewayId = resolveGatewayId(request.getGatewayId());
        gatewayManageAppService.ensureGatewayExists(gatewayId);

        Map<String, Object> arguments = request.getArguments() == null ? Collections.emptyMap() : request.getArguments();
        GatewayToolService.ToolCallResult callResult = gatewayToolService.callTool(gatewayId, request.getToolName(), arguments);

        GatewayToolDebugResponse data = GatewayToolDebugResponse.builder()
                .success(callResult.success())
                .content(callResult.content())
                .errorCode(callResult.errorCode())
                .build();
        return Result.success(data);
    }

    /**
     * 查询模型与工具的绑定关系。
     * 流程：
     * 1. 进入接口后执行 `tool:read` 权限校验。
     * 2. Spring 绑定请求体并校验 `modelId` 非空。
     * 3. 查询 MODEL 维度绑定记录并过滤掉禁用项。
     * 4. 汇总 toolId 列表并计算 `globalVisible` 标记。
     * 5. 统一封装 `Result.success` 返回。
     *
     * @param request 网关管理查询参数。
     */
    @PostMapping("/bindings/model/get")
    @SaCheckPermission("tool:read")
    @Override
    public Result<GatewayModelBindingResponse> getModelBindings(@RequestBody ModelBindingQueryRequest request) {
        if (request == null || request.getModelId() == null) {
            throw new IllegalArgumentException("modelId 不能为空");
        }
        List<McpToolBinding> bindings = gatewayManageAppService.queryModelBindings(request.getModelId());

        List<Long> toolIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(bindings)) {
            for (McpToolBinding binding : bindings) {
                if (binding != null && binding.getToolId() != null && !Boolean.FALSE.equals(binding.getEnabled())) {
                    toolIds.add(binding.getToolId());
                }
            }
        }

        GatewayModelBindingResponse data = GatewayModelBindingResponse.builder()
                .modelId(request.getModelId())
                .toolIds(toolIds)
                .globalVisible(toolIds.isEmpty())
                .build();
        return Result.success(data);
    }

    /**
     * 覆盖保存模型与工具绑定关系。
     * 流程：
     * 1. 进入接口后执行 `tool:write` 权限校验。
     * 2. Spring 绑定请求体并校验 `modelId` 非空。
     * 3. 先删除该模型历史绑定，保证保存语义为"全量覆盖"。
     * 4. 遍历 toolIds 校验工具存在后逐条创建绑定记录。
     * 5. 统一封装空成功结果返回。
     *
     * @param request 网关管理保存参数。
     * @return 模型工具绑定保存状态。
     */
    @PostMapping("/bindings/model/save")
    @SaCheckPermission("tool:write")
    @Override
    public Result<Void> saveModelBindings(@RequestBody SaveModelBindingRequest request) {
        if (request == null || request.getModelId() == null) {
            throw new IllegalArgumentException("modelId 不能为空");
        }
        gatewayManageAppService.saveModelBindings(request.getModelId(), request.getToolIds());
        return Result.success();
    }

    /**
     * 查询全部已启用工具。
     * 流程：
     * 1. 进入接口后执行 `tool:read` 权限校验。
     * 2. 查询所有启用网关实例。
     * 3. 按网关逐个查询启用工具列表。
     * 4. 转换为统一的简化工具视图集合。
     * 5. 统一封装 `Result.success` 返回。
     */
    @PostMapping("/tools/all-enabled")
    @SaCheckPermission("tool:read")
    @Override
    public Result<List<GatewayToolOptionResponse>> allEnabledTools() {
        List<McpToolRegistry> tools = gatewayManageAppService.listAllEnabledTools();
        List<GatewayToolOptionResponse> result = new ArrayList<>();
        for (McpToolRegistry tool : tools) {
            result.add(GatewayToolOptionResponse.builder()
                    .id(tool.getId())
                    .gatewayId(tool.getGatewayId())
                    .toolName(tool.getToolName())
                    .toolDescription(tool.getToolDescription())
                    .build());
        }
        return Result.success(result);
    }

    /**
     * 查询已启用模型列表。
     * 流程：
     * 1. 进入接口后执行 `tool:read` 权限校验。
     * 2. 查询所有 `enabled=true` 的模型配置。
     * 3. 逐条提取 id、modelName、modelType 关键字段。
     * 4. 组装前端使用的模型下拉数据结构。
     * 5. 统一封装 `Result.success` 返回。
     */
    @PostMapping("/models/enabled")
    @SaCheckPermission("tool:read")
    @Override
    public Result<List<GatewayModelOptionResponse>> enabledModels() {
        List<ModelConfig> models = gatewayManageAppService.listEnabledModels();
        List<GatewayModelOptionResponse> result = new ArrayList<>();
        for (ModelConfig model : models) {
            result.add(GatewayModelOptionResponse.builder()
                    .id(model.getId())
                    .modelName(model.getModelName())
                    .modelType(model.getModelType())
                    .build());
        }
        return Result.success(result);
    }

    /**
     * 查询网关监控指标总览。
     * 流程：
     * 1. 进入接口后执行 `tool:read` 权限校验。
     * 2. Spring 绑定请求体并解析 gatewayId/toolName/recentMinutes。
     * 3. Controller 组装 `MetricsQuery` 调用观测应用服务。
     * 4. 将报告对象转换为 `generatedAt/toolMetrics/alerts` 结构。
     * 5. 统一封装 `Result.success` 返回。
     *
     * @param request 网关管理查询参数。
     */
    @PostMapping("/metrics/overview")
    @SaCheckPermission("tool:read")
    @Override
    public Result<GatewayMetricsResponse> queryGatewayMetrics(@RequestBody GatewayMetricsQueryRequest request) {
        GatewayObservabilityAppService.GatewayMetricsReport report = gatewayObservabilityAppService.queryMetrics(
                new GatewayObservabilityAppService.MetricsQuery(
                        resolveGatewayId(request == null ? null : request.getGatewayId()),
                        request == null ? null : request.getToolName(),
                        request == null ? null : request.getRecentMinutes()
                )
        );
        List<GatewayMetricsResponse.ToolMetricsSnapshot> toolMetrics = new ArrayList<>();
        if (!CollectionUtils.isEmpty(report.toolMetrics())) {
            for (GatewayObservabilityAppService.ToolMetricsSnapshot metric : report.toolMetrics()) {
                toolMetrics.add(GatewayMetricsResponse.ToolMetricsSnapshot.builder()
                        .gatewayId(metric.gatewayId())
                        .toolName(metric.toolName())
                        .requestCount(metric.requestCount())
                        .successRate(metric.successRate())
                        .p95LatencyMs(metric.p95LatencyMs())
                        .p99LatencyMs(metric.p99LatencyMs())
                        .avgLatencyMs(metric.avgLatencyMs())
                        .errorDistribution(metric.errorDistribution())
                        .slaRate(metric.slaRate())
                        .timeoutRate(metric.timeoutRate())
                        .consecutiveFailures(metric.consecutiveFailures())
                        .latestCallAt(metric.latestCallAt())
                        .latestCallSuccess(metric.latestCallSuccess())
                        .build());
            }
        }

        List<GatewayMetricsResponse.AlertSnapshot> alerts = new ArrayList<>();
        if (!CollectionUtils.isEmpty(report.alerts())) {
            for (GatewayObservabilityAppService.AlertSnapshot alert : report.alerts()) {
                alerts.add(GatewayMetricsResponse.AlertSnapshot.builder()
                        .alertType(alert.alertType())
                        .level(alert.level())
                        .gatewayId(alert.gatewayId())
                        .toolName(alert.toolName())
                        .message(alert.message())
                        .triggeredAt(alert.triggeredAt())
                        .build());
            }
        }

        GatewayMetricsResponse data = GatewayMetricsResponse.builder()
                .generatedAt(report.generatedAt())
                .recentMinutes(report.recentMinutes())
                .toolMetrics(toolMetrics)
                .alerts(alerts)
                .build();
        return Result.success(data);
    }

    /**
     * 刷新工具连通性状态。
     * 流程：
     * 1. 进入接口后执行 `tool:write` 权限校验。
     * 2. Spring 绑定请求体并解析 gatewayId/toolId。
     * 3. 根据是否指定 toolId 决定刷新单个工具或网关下全部工具。
     * 4. 逐个调用 HTTP 接口进行连通性测试并更新状态。
     * 5. 统计成功/失败数量并统一封装返回。
     *
     * @param request 工具刷新参数（gatewayId/toolId）。
     * @return 刷新结果（包含 refreshedCount/successCount/failedCount/details）
     */
    @PostMapping("/tools/refresh")
    @SaCheckPermission("tool:write")
    @Override
    public Result<GatewayToolRefreshResponse> refreshTools(@RequestBody RefreshToolsRequest request) {
        String gatewayId = resolveGatewayId(request == null ? null : request.getGatewayId());
        gatewayManageAppService.ensureGatewayExists(gatewayId);
        Long toolId = request == null ? null : request.getToolId();
        List<McpToolRegistry> toolsToRefresh = gatewayManageAppService.queryToolsForRefresh(gatewayId, toolId);

        if (CollectionUtils.isEmpty(toolsToRefresh)) {
            GatewayToolRefreshResponse emptyResult = GatewayToolRefreshResponse.builder()
                    .gatewayId(gatewayId)
                    .refreshedCount(0)
                    .successCount(0)
                    .failedCount(0)
                    .details(Collections.emptyList())
                    .build();
            return Result.success("没有需要刷新的工具", emptyResult);
        }

        List<GatewayToolRefreshResponse.RefreshDetail> details = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;

        for (McpToolRegistry tool : toolsToRefresh) {
            if (tool == null) {
                continue;
            }
            GatewayToolRefreshResponse.RefreshDetail.RefreshDetailBuilder detailBuilder =
                    GatewayToolRefreshResponse.RefreshDetail.builder()
                            .toolId(tool.getId())
                            .toolName(tool.getToolName())
                            .httpMethod(tool.getHttpMethod())
                            .httpUrl(tool.getHttpUrl());

            try {
                // 执行 HTTP 连通性测试（HEAD 请求或超时限制的 GET 请求）
                boolean isReachable = testHttpConnectivity(tool);
                detailBuilder.reachable(isReachable)
                        .message(isReachable ? "连通" : "不可达");

                if (isReachable) {
                    successCount++;
                } else {
                    failedCount++;
                }
            } catch (Exception e) {
                log.warn("工具连通性测试失败，toolId: {}, toolName: {}", tool.getId(), tool.getToolName(), e);
                detailBuilder.reachable(false)
                        .error(e.getMessage())
                        .message("不可达");
                failedCount++;
            }
            details.add(detailBuilder.build());
        }

        GatewayToolRefreshResponse result = GatewayToolRefreshResponse.builder()
                .gatewayId(gatewayId)
                .refreshedCount(toolsToRefresh.size())
                .successCount(successCount)
                .failedCount(failedCount)
                .details(details)
                .build();

        String message = String.format("刷新完成：成功 %d 个，失败 %d 个", successCount, failedCount);
        return Result.success(message, result);
    }

    /**
     * 测试 HTTP 工具连通性。
     *
     * @param tool 工具注册记录
     * @return 是否可达
     */
    private boolean testHttpConnectivity(McpToolRegistry tool) {
        if (!StringUtils.hasText(tool.getHttpUrl())) {
            return false;
        }
        int timeout = tool.getTimeout() == null ? 5000 : tool.getTimeout();
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofMillis(timeout))
                    .build();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(tool.getHttpUrl()))
                    .method(tool.getHttpMethod() != null ? tool.getHttpMethod() : "GET",
                            java.net.http.HttpRequest.BodyPublishers.noBody())
                    .timeout(java.time.Duration.ofMillis(timeout))
                    .build();
            java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            // 2xx 状态码表示连通
            return response.statusCode() >= 200 && response.statusCode() < 400;
        } catch (Exception e) {
            log.debug("HTTP 连通性测试失败：url={}, error={}", tool.getHttpUrl(), e.getMessage());
            return false;
        }
    }

    private GatewayInstanceResponse toGatewayResponse(McpGateway gateway, Long toolCount) {
        if (gateway == null) {
            return GatewayInstanceResponse.builder().build();
        }
        return GatewayInstanceResponse.builder()
                .id(gateway.getId())
                .gatewayId(gateway.getGatewayId())
                .gatewayName(gateway.getGatewayName())
                .gatewayDesc(gateway.getGatewayDesc())
                .gatewayVersion(gateway.getGatewayVersion())
                .gatewayInstructions(gateway.getGatewayInstructions())
                .status(gateway.getStatus())
                .toolCount(toolCount)
                .createdAt(gateway.getCreatedAt())
                .updatedAt(gateway.getUpdatedAt())
                .build();
    }

    private GatewayAuthResponse toGatewayAuthResponse(McpGatewayAuth auth) {
        if (auth == null) {
            return GatewayAuthResponse.builder().build();
        }
        return GatewayAuthResponse.builder()
                .id(auth.getId())
                .gatewayId(auth.getGatewayId())
                .apiKey(auth.getApiKey())
                .rateLimit(auth.getRateLimit())
                .expireTime(auth.getExpireTime())
                .status(auth.getStatus())
                .createdAt(auth.getCreatedAt())
                .updatedAt(auth.getUpdatedAt())
                .build();
    }

    private GatewayToolResponse toToolResponse(McpToolRegistry tool, String lastCallSummary) {
        if (tool == null) {
            return GatewayToolResponse.builder().build();
        }
        return GatewayToolResponse.builder()
                .id(tool.getId())
                .gatewayId(tool.getGatewayId())
                .toolName(tool.getToolName())
                .toolKey(tool.getToolKey())
                .toolDescription(tool.getToolDescription())
                .httpMethod(tool.getHttpMethod())
                .httpUrl(tool.getHttpUrl())
                .httpHeaders(tool.getHttpHeaders())
                .timeout(tool.getTimeout())
                .retryTimes(tool.getRetryTimes())
                .riskLevel(tool.getRiskLevel())
                .status(tool.getStatus())
                .lastCallSummary(lastCallSummary)
                .createdAt(tool.getCreatedAt())
                .updatedAt(tool.getUpdatedAt())
                .build();
    }

    private GatewayToolMappingResponse toToolMappingResponse(McpToolMapping mapping) {
        if (mapping == null) {
            return GatewayToolMappingResponse.builder().build();
        }
        return GatewayToolMappingResponse.builder()
                .id(mapping.getId())
                .gatewayId(mapping.getGatewayId())
                .toolId(mapping.getToolId())
                .mappingType(mapping.getMappingType())
                .parentId(mapping.getParentId())
                .fieldName(mapping.getFieldName())
                .mcpType(mapping.getMcpType())
                .mcpDesc(mapping.getMcpDesc())
                .isRequired(mapping.getIsRequired())
                .itemType(mapping.getItemType())
                .itemRefId(mapping.getItemRefId())
                .httpPath(mapping.getHttpPath())
                .httpLocation(mapping.getHttpLocation())
                .sortOrder(mapping.getSortOrder())
                .createdAt(mapping.getCreatedAt())
                .updatedAt(mapping.getUpdatedAt())
                .build();
    }

    private List<GatewayManageAppService.ToolMappingNode> toToolMappingNodes(List<MappingNodeRequest> mappings) {
        List<GatewayManageAppService.ToolMappingNode> nodes = new ArrayList<>();
        if (CollectionUtils.isEmpty(mappings)) {
            return nodes;
        }
        for (MappingNodeRequest mapping : mappings) {
            nodes.add(toToolMappingNode(mapping));
        }
        return nodes;
    }

    private GatewayManageAppService.ToolMappingNode toToolMappingNode(MappingNodeRequest mapping) {
        if (mapping == null) {
            return null;
        }
        return GatewayManageAppService.ToolMappingNode.builder()
                .parentId(mapping.getParentId())
                .fieldName(mapping.getFieldName())
                .mcpType(mapping.getMcpType())
                .mcpDesc(mapping.getMcpDesc())
                .isRequired(mapping.getIsRequired())
                .itemType(mapping.getItemType())
                .itemRefId(mapping.getItemRefId())
                .httpPath(mapping.getHttpPath())
                .httpLocation(mapping.getHttpLocation())
                .sortOrder(mapping.getSortOrder())
                .children(toToolMappingNodes(mapping.getChildren()))
                .build();
    }

    private String resolveGatewayId(String gatewayId) {
        if (!StringUtils.hasText(gatewayId)) {
            return DEFAULT_GATEWAY_ID;
        }
        return gatewayId.trim();
    }

    /**
     * 执行状态更新。
     *
     * @param id 主键 ID。
     * @param status 状态值。
     * @param updater 状态更新执行器。
     * @return 更新结果。
     */
    private Result<Void> executeStatusUpdate(Long id, int status, BiConsumer<Long, Integer> updater) {
        updater.accept(id, status);
        return Result.success();
    }

}
