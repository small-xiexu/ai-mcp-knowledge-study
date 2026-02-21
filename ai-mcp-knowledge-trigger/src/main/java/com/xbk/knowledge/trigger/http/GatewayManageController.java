package com.xbk.knowledge.trigger.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.application.service.app.GatewayManageAppService;
import com.xbk.knowledge.application.service.app.GatewayObservabilityAppService;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.gateway.model.entity.McpGateway;
import com.xbk.knowledge.domain.gateway.model.entity.McpGatewayAuth;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolBinding;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolMapping;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolRegistry;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayIdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayPageQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolBindingQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolMappingQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolNameQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolRegistryPageQuery;
import com.xbk.knowledge.domain.llm.adapter.repository.ModelConfigRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpGatewayAuthRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpGatewayRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolBindingRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolMappingRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolRegistryRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolSchemaRepository;
import com.xbk.knowledge.domain.gateway.service.GatewayToolService;
import com.xbk.knowledge.types.common.PageRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.types.enums.ToolBindType;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gateway 管理接口
 *
 * @author sxie
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gateway/manage")
public class GatewayManageController {

    private static final String DEFAULT_GATEWAY_ID = "default_gateway";
    private static final String CALL_ID_MDC_KEY = "gatewayToolCallId";

    private final McpGatewayRepository gatewayRepository;
    private final McpGatewayAuthRepository gatewayAuthRepository;
    private final McpToolRegistryRepository toolRegistryRepository;
    private final McpToolMappingRepository toolMappingRepository;
    private final McpToolBindingRepository toolBindingRepository;
    private final McpToolSchemaRepository toolSchemaRepository;
    private final ModelConfigRepository modelConfigRepository;
    private final GatewayToolService gatewayToolService;
    private final GatewayObservabilityAppService gatewayObservabilityAppService;
    private final GatewayManageAppService gatewayManageAppService;

    /**
     * listGatewayInstances。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/instances/list")
    @SaCheckPermission("tool:read")
    public Result<PageResult<Map<String, Object>>> listGatewayInstances(@Valid @RequestBody PageRequest request) {
        int offset = request.getOffset();
        int pageSize = request.getPageSize();
        GatewayPageQuery query = new GatewayPageQuery(offset, pageSize);
        List<McpGateway> gateways = gatewayRepository.findPage(query);
        long total = gatewayRepository.countAll();

        List<Map<String, Object>> records = new ArrayList<>();
        for (McpGateway gateway : gateways) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", gateway.getId());
            row.put("gatewayId", gateway.getGatewayId());
            row.put("gatewayName", gateway.getGatewayName());
            row.put("gatewayVersion", gateway.getGatewayVersion());
            row.put("status", gateway.getStatus());
            row.put("toolCount", toolRegistryRepository.countByGatewayId(new GatewayIdQuery(gateway.getGatewayId())));
            row.put("createdAt", gateway.getCreatedAt());
            row.put("updatedAt", gateway.getUpdatedAt());
            records.add(row);
        }

        return Result.success(PageResult.of(records, total, request.getPageNum(), request.getPageSize()));
    }

    /**
     * saveGatewayInstance。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/instances/save")
    @SaCheckPermission("tool:write")
    public Result<McpGateway> saveGatewayInstance(@RequestBody GatewayInstanceRequest request) {
        if (!StringUtils.hasText(request.getGatewayName())) {
            return Result.error("gatewayName 不能为空");
        }
        String gatewayId = StringUtils.hasText(request.getGatewayId())
                ? request.getGatewayId().trim()
                : DEFAULT_GATEWAY_ID;

        McpGateway gateway;
        if (request.getId() == null) {
            gateway = gatewayRepository.findByGatewayId(new GatewayIdQuery(gatewayId)).orElse(null);
            if (gateway == null) {
                gateway = new McpGateway();
                gateway.setCreatedAt(LocalDateTime.now());
            }
        } else {
            gateway = gatewayRepository.findById(new IdQuery(request.getId())).orElse(new McpGateway());
            gateway.setId(request.getId());
        }

        gateway.setGatewayId(gatewayId);
        gateway.setGatewayName(request.getGatewayName());
        gateway.setGatewayDesc(request.getGatewayDesc());
        gateway.setGatewayVersion(request.getGatewayVersion());
        gateway.setGatewayInstructions(request.getGatewayInstructions());
        gateway.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        gateway.setUpdatedAt(LocalDateTime.now());

        McpGateway saved = gatewayRepository.save(gateway);
        return Result.success(saved);
    }

    /**
     * deleteGatewayInstance。
     *
     * @param query 参数
     * @return 返回结果
     */
    @PostMapping("/instances/delete")
    @SaCheckPermission("tool:write")
    public Result<Void> deleteGatewayInstance(@RequestBody IdQuery query) {
        if (query == null || query.getId() == null) {
            return Result.error("ID 不能为空");
        }
        gatewayManageAppService.deleteGatewayInstance(query);
        return Result.success();
    }

    /**
     * listGatewayAuth。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/auth/list")
    @SaCheckPermission("tool:read")
    public Result<PageResult<Map<String, Object>>> listGatewayAuth(@RequestBody GatewayAuthListRequest request) {
        String gatewayId = resolveGatewayId(request == null ? null : request.getGatewayId());
        ensureGatewayExists(gatewayId);

        Integer status = request == null ? null : request.getStatus();
        String apiKeyKeyword = request == null ? null : request.getApiKeyKeyword();
        String keyword = StringUtils.hasText(apiKeyKeyword) ? apiKeyKeyword.trim() : null;

        List<McpGatewayAuth> authList = gatewayAuthRepository.findByGatewayId(new GatewayIdQuery(gatewayId));
        List<McpGatewayAuth> filtered = new ArrayList<>();
        for (McpGatewayAuth auth : authList) {
            if (auth == null) {
                continue;
            }
            if (status != null && !status.equals(auth.getStatus())) {
                continue;
            }
            if (StringUtils.hasText(keyword)) {
                String currentApiKey = auth.getApiKey();
                if (!StringUtils.hasText(currentApiKey) || !currentApiKey.contains(keyword)) {
                    continue;
                }
            }
            filtered.add(auth);
        }

        int pageNum = request == null || request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        int pageSize = request == null || request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();
        int start = Math.min((pageNum - 1) * pageSize, filtered.size());
        int end = Math.min(start + pageSize, filtered.size());

        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = start; i < end; i++) {
            McpGatewayAuth auth = filtered.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", auth.getId());
            row.put("gatewayId", auth.getGatewayId());
            row.put("apiKey", auth.getApiKey());
            row.put("rateLimit", auth.getRateLimit());
            row.put("expireTime", auth.getExpireTime());
            row.put("status", auth.getStatus());
            row.put("createdAt", auth.getCreatedAt());
            row.put("updatedAt", auth.getUpdatedAt());
            records.add(row);
        }

        return Result.success(PageResult.of(records, (long) filtered.size(), pageNum, pageSize));
    }

    /**
     * saveGatewayAuth。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/auth/save")
    @SaCheckPermission("tool:write")
    public Result<McpGatewayAuth> saveGatewayAuth(@RequestBody SaveGatewayAuthRequest request) {
        if (request == null) {
            return Result.error("请求参数不能为空");
        }

        McpGatewayAuth auth;
        String gatewayId;
        if (request.getId() == null) {
            gatewayId = resolveGatewayId(request.getGatewayId());
            auth = new McpGatewayAuth();
            auth.setCreatedAt(LocalDateTime.now());
        } else {
            auth = gatewayAuthRepository.findById(request.getId()).orElse(null);
            if (auth == null) {
                return Result.error("凭证不存在");
            }
            gatewayId = StringUtils.hasText(request.getGatewayId())
                    ? resolveGatewayId(request.getGatewayId())
                    : auth.getGatewayId();
            auth.setId(request.getId());
        }
        ensureGatewayExists(gatewayId);

        String apiKey = resolveApiKey(request.getApiKey(), auth.getApiKey(), request.getId() == null);
        if (!StringUtils.hasText(apiKey)) {
            return Result.error("apiKey 不能为空");
        }

        List<McpGatewayAuth> existingAuthList = gatewayAuthRepository.findByGatewayId(new GatewayIdQuery(gatewayId));
        for (McpGatewayAuth existing : existingAuthList) {
            if (existing == null || existing.getId() == null || !apiKey.equals(existing.getApiKey())) {
                continue;
            }
            if (request.getId() == null || !existing.getId().equals(request.getId())) {
                return Result.error("同一网关下 API Key 已存在");
            }
        }

        auth.setGatewayId(gatewayId);
        auth.setApiKey(apiKey);
        auth.setRateLimit(resolveRateLimit(request.getRateLimit()));
        auth.setExpireTime(request.getExpireTime());
        auth.setStatus(resolveStatus(request.getStatus()));
        auth.setUpdatedAt(LocalDateTime.now());

        McpGatewayAuth saved = gatewayAuthRepository.save(auth);
        return Result.success(saved);
    }

    /**
     * enableGatewayAuth。
     *
     * @param query 参数
     * @return 返回结果
     */
    @PostMapping("/auth/enable")
    @SaCheckPermission("tool:write")
    public Result<Void> enableGatewayAuth(@RequestBody IdQuery query) {
        return updateGatewayAuthStatus(query, 1);
    }

    /**
     * disableGatewayAuth。
     *
     * @param query 参数
     * @return 返回结果
     */
    @PostMapping("/auth/disable")
    @SaCheckPermission("tool:write")
    public Result<Void> disableGatewayAuth(@RequestBody IdQuery query) {
        return updateGatewayAuthStatus(query, 0);
    }

    /**
     * listTools。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/tools/list")
    @SaCheckPermission("tool:read")
    public Result<PageResult<Map<String, Object>>> listTools(@RequestBody ToolListRequest request) {
        String gatewayId = resolveGatewayId(request == null ? null : request.getGatewayId());
        ensureGatewayExists(gatewayId);
        int pageNum = request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();
        int offset = (pageNum - 1) * pageSize;

        List<McpToolRegistry> records = toolRegistryRepository.findPage(new ToolRegistryPageQuery(gatewayId, offset, pageSize));
        long total = toolRegistryRepository.countByGatewayId(new GatewayIdQuery(gatewayId));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (McpToolRegistry tool : records) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", tool.getId());
            row.put("gatewayId", tool.getGatewayId());
            row.put("toolName", tool.getToolName());
            row.put("toolDescription", tool.getToolDescription());
            row.put("httpMethod", tool.getHttpMethod());
            row.put("httpUrl", tool.getHttpUrl());
            row.put("status", tool.getStatus());
            row.put("timeout", tool.getTimeout());
            row.put("retryTimes", tool.getRetryTimes());
            row.put("lastCallSummary", "-");
            row.put("createdAt", tool.getCreatedAt());
            row.put("updatedAt", tool.getUpdatedAt());
            rows.add(row);
        }

        return Result.success(PageResult.of(rows, total, pageNum, pageSize));
    }

    /**
     * getTool。
     *
     * @param query 参数
     * @return 返回结果
     */
    @PostMapping("/tools/get")
    @SaCheckPermission("tool:read")
    public Result<Map<String, Object>> getTool(@RequestBody IdQuery query) {
        McpToolRegistry tool = toolRegistryRepository.findById(query).orElse(null);
        if (tool == null) {
            return Result.error("工具不存在");
        }

        List<McpToolMapping> requestMappings = toolMappingRepository.findByToolIdAndMappingType(
                new ToolMappingQuery(tool.getId(), "request")
        );
        List<McpToolMapping> responseMappings = toolMappingRepository.findByToolIdAndMappingType(
                new ToolMappingQuery(tool.getId(), "response")
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tool", tool);
        data.put("requestMappings", requestMappings);
        data.put("responseMappings", responseMappings);
        return Result.success(data);
    }

    /**
     * saveTool。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/tools/save")
    @SaCheckPermission("tool:write")
    public Result<McpToolRegistry> saveTool(@RequestBody SaveToolRequest request) {
        if (!StringUtils.hasText(request.getToolName())) {
            return Result.error("toolName 不能为空");
        }
        String gatewayId = resolveGatewayId(request.getGatewayId());
        ensureGatewayExists(gatewayId);

        McpToolRegistry tool;
        if (request.getId() == null) {
            tool = new McpToolRegistry();
            tool.setCreatedAt(LocalDateTime.now());
        } else {
            tool = toolRegistryRepository.findById(new IdQuery(request.getId())).orElse(new McpToolRegistry());
            tool.setId(request.getId());
        }

        tool.setGatewayId(gatewayId);
        tool.setToolName(request.getToolName());
        tool.setToolDescription(request.getToolDescription());
        tool.setHttpMethod(request.getHttpMethod());
        tool.setHttpUrl(request.getHttpUrl());
        tool.setHttpHeaders(request.getHttpHeaders());
        tool.setTimeout(request.getTimeout() == null ? 30000 : request.getTimeout());
        tool.setRetryTimes(request.getRetryTimes() == null ? 0 : request.getRetryTimes());
        tool.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        tool.setUpdatedAt(LocalDateTime.now());

        McpToolRegistry saved = toolRegistryRepository.save(tool);
        toolMappingRepository.deleteByToolId(saved.getId());
        toolSchemaRepository.deleteByToolId(saved.getId());
        saveMappings(saved.getId(), saved.getGatewayId(), request.getRequestMappings(), "request");
        saveMappings(saved.getId(), saved.getGatewayId(), request.getResponseMappings(), "response");

        return Result.success(saved);
    }

    /**
     * deleteTool。
     *
     * @param query 参数
     * @return 返回结果
     */
    @PostMapping("/tools/delete")
    @SaCheckPermission("tool:write")
    public Result<Void> deleteTool(@RequestBody IdQuery query) {
        if (query == null || query.getId() == null) {
            return Result.error("ID 不能为空");
        }
        gatewayManageAppService.deleteTool(query);
        return Result.success();
    }

    /**
     * enableTool。
     *
     * @param query 参数
     * @return 返回结果
     */
    @PostMapping("/tools/enable")
    @SaCheckPermission("tool:write")
    public Result<Void> enableTool(@RequestBody IdQuery query) {
        McpToolRegistry tool = toolRegistryRepository.findById(query).orElse(null);
        if (tool == null) {
            return Result.error("工具不存在");
        }
        tool.setStatus(1);
        tool.setUpdatedAt(LocalDateTime.now());
        toolRegistryRepository.save(tool);
        return Result.success();
    }

    /**
     * disableTool。
     *
     * @param query 参数
     * @return 返回结果
     */
    @PostMapping("/tools/disable")
    @SaCheckPermission("tool:write")
    public Result<Void> disableTool(@RequestBody IdQuery query) {
        McpToolRegistry tool = toolRegistryRepository.findById(query).orElse(null);
        if (tool == null) {
            return Result.error("工具不存在");
        }
        tool.setStatus(0);
        tool.setUpdatedAt(LocalDateTime.now());
        toolRegistryRepository.save(tool);
        return Result.success();
    }

    /**
     * debugTool。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/tools/debug")
    @SaCheckPermission("tool:invoke")
    public Result<Map<String, Object>> debugTool(@RequestBody ToolDebugRequest request) {
        if (!StringUtils.hasText(request.getToolName())) {
            return Result.error("toolName 不能为空");
        }
        String gatewayId = resolveGatewayId(request.getGatewayId());
        ensureGatewayExists(gatewayId);

        Map<String, Object> arguments = request.getArguments() == null ? Collections.emptyMap() : request.getArguments();
        String callId = UUID.randomUUID().toString().replace("-", "");
        long startAt = System.nanoTime();
        String previousCallId = MDC.get(CALL_ID_MDC_KEY);
        MDC.put(CALL_ID_MDC_KEY, callId);
        log.info("gateway_tool_call source=DEBUG stage=start callId={} gatewayId={} toolName={} argsKeys={}",
                callId,
                gatewayId,
                request.getToolName(),
                arguments.keySet());
        GatewayToolService.ToolCallResult callResult;
        try {
            callResult = gatewayToolService.callTool(gatewayId, request.getToolName(), arguments);
        } finally {
            if (StringUtils.hasText(previousCallId)) {
                MDC.put(CALL_ID_MDC_KEY, previousCallId);
            } else {
                MDC.remove(CALL_ID_MDC_KEY);
            }
        }
        long latencyMs = (System.nanoTime() - startAt) / 1_000_000;
        log.info("gateway_tool_call source=DEBUG stage=end callId={} gatewayId={} toolName={} argsKeys={} success={} errorCode={} latencyMs={}",
                callId,
                gatewayId,
                request.getToolName(),
                arguments.keySet(),
                callResult.success(),
                callResult.errorCode(),
                latencyMs);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", callResult.success());
        data.put("content", callResult.content());
        data.put("errorCode", callResult.errorCode());
        return Result.success(data);
    }

    /**
     * getModelBindings。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/bindings/model/get")
    @SaCheckPermission("tool:read")
    public Result<Map<String, Object>> getModelBindings(@RequestBody ModelBindingQueryRequest request) {
        if (request == null || request.getModelId() == null) {
            return Result.error("modelId 不能为空");
        }
        List<McpToolBinding> bindings = toolBindingRepository.findByBindTypeAndTargetId(
                new ToolBindingQuery(ToolBindType.MODEL.name(), request.getModelId())
        );

        List<Long> toolIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(bindings)) {
            for (McpToolBinding binding : bindings) {
                if (binding != null && binding.getToolId() != null && !Boolean.FALSE.equals(binding.getEnabled())) {
                    toolIds.add(binding.getToolId());
                }
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("modelId", request.getModelId());
        data.put("toolIds", toolIds);
        data.put("globalVisible", toolIds.isEmpty());
        return Result.success(data);
    }

    /**
     * saveModelBindings。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/bindings/model/save")
    @SaCheckPermission("tool:write")
    public Result<Void> saveModelBindings(@RequestBody SaveModelBindingRequest request) {
        if (request == null || request.getModelId() == null) {
            return Result.error("modelId 不能为空");
        }

        List<McpToolBinding> existing = toolBindingRepository.findByBindTypeAndTargetId(
                new ToolBindingQuery(ToolBindType.MODEL.name(), request.getModelId())
        );
        for (McpToolBinding binding : existing) {
            if (binding != null && binding.getId() != null) {
                toolBindingRepository.deleteById(binding.getId());
            }
        }

        if (CollectionUtils.isEmpty(request.getToolIds())) {
            return Result.success();
        }

        for (Long toolId : request.getToolIds()) {
            if (toolId == null) {
                continue;
            }
            McpToolRegistry tool = toolRegistryRepository.findById(new IdQuery(toolId)).orElse(null);
            if (tool == null) {
                continue;
            }
            McpToolBinding binding = McpToolBinding.builder()
                    .gatewayId(tool.getGatewayId())
                    .toolId(toolId)
                    .bindType(ToolBindType.MODEL.name())
                    .bindTargetId(request.getModelId())
                    .enabled(Boolean.TRUE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            toolBindingRepository.save(binding);
        }

        return Result.success();
    }

    /**
     * allEnabledTools。
     *
     * @return 返回结果
     */
    @PostMapping("/tools/all-enabled")
    @SaCheckPermission("tool:read")
    public Result<List<Map<String, Object>>> allEnabledTools() {
        List<McpGateway> gateways = gatewayRepository.findAllEnabled();
        List<Map<String, Object>> tools = new ArrayList<>();
        for (McpGateway gateway : gateways) {
            List<McpToolRegistry> gatewayTools = toolRegistryRepository.findEnabledByGatewayId(
                    new GatewayIdQuery(gateway.getGatewayId())
            );
            for (McpToolRegistry tool : gatewayTools) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", tool.getId());
                item.put("gatewayId", tool.getGatewayId());
                item.put("toolName", tool.getToolName());
                item.put("toolDescription", tool.getToolDescription());
                tools.add(item);
            }
        }
        return Result.success(tools);
    }

    /**
     * enabledModels。
     *
     * @return 返回结果
     */
    @PostMapping("/models/enabled")
    @SaCheckPermission("tool:read")
    public Result<List<Map<String, Object>>> enabledModels() {
        List<ModelConfig> models = modelConfigRepository.findByEnabled(new EnabledQuery(true));
        List<Map<String, Object>> result = new ArrayList<>();
        for (ModelConfig model : models) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", model.getId());
            item.put("modelName", model.getModelName());
            item.put("modelType", model.getModelType());
            result.add(item);
        }
        return Result.success(result);
    }

    /**
     * queryGatewayMetrics。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/metrics/overview")
    @SaCheckPermission("tool:read")
    public Result<Map<String, Object>> queryGatewayMetrics(@RequestBody GatewayMetricsQueryRequest request) {
        GatewayObservabilityAppService.GatewayMetricsReport report = gatewayObservabilityAppService.queryMetrics(
                new GatewayObservabilityAppService.MetricsQuery(
                        resolveGatewayId(request == null ? null : request.getGatewayId()),
                        request == null ? null : request.getToolName(),
                        request == null ? null : request.getRecentMinutes()
                )
        );
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("generatedAt", report.generatedAt());
        data.put("recentMinutes", report.recentMinutes());
        data.put("toolMetrics", report.toolMetrics());
        data.put("alerts", report.alerts());
        return Result.success(data);
    }

    private void saveMappings(Long toolId,
                              String gatewayId,
                              List<MappingNodeRequest> mappings,
                              String mappingType) {
        if (CollectionUtils.isEmpty(mappings)) {
            return;
        }

        int[] sortOrder = new int[]{0};
        for (MappingNodeRequest mapping : mappings) {
            saveMappingNode(toolId, gatewayId, mappingType, mapping, null, sortOrder);
        }
    }

    private void saveMappingNode(Long toolId,
                                 String gatewayId,
                                 String mappingType,
                                 MappingNodeRequest mapping,
                                 Long parentId,
                                 int[] sortOrder) {
        if (mapping == null || !StringUtils.hasText(mapping.getFieldName())) {
            return;
        }
        McpToolMapping entity = McpToolMapping.builder()
                .gatewayId(gatewayId)
                .toolId(toolId)
                .mappingType(mappingType)
                .parentId(parentId == null ? mapping.getParentId() : parentId)
                .fieldName(mapping.getFieldName())
                .mcpType(mapping.getMcpType())
                .mcpDesc(mapping.getMcpDesc())
                .isRequired(mapping.getIsRequired())
                .itemType(mapping.getItemType())
                .itemRefId(mapping.getItemRefId())
                .httpPath(mapping.getHttpPath())
                .httpLocation(mapping.getHttpLocation())
                .sortOrder(mapping.getSortOrder() == null ? sortOrder[0] : mapping.getSortOrder())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        McpToolMapping saved = toolMappingRepository.save(entity);
        sortOrder[0]++;

        if (!CollectionUtils.isEmpty(mapping.getChildren())) {
            for (MappingNodeRequest child : mapping.getChildren()) {
                saveMappingNode(toolId, gatewayId, mappingType, child, saved.getId(), sortOrder);
            }
        }
    }

    private String resolveGatewayId(String gatewayId) {
        if (!StringUtils.hasText(gatewayId)) {
            return DEFAULT_GATEWAY_ID;
        }
        return gatewayId.trim();
    }

    private McpGateway ensureGatewayExists(String gatewayId) {
        String resolvedGatewayId = resolveGatewayId(gatewayId);
        McpGateway existing = gatewayRepository.findByGatewayId(new GatewayIdQuery(resolvedGatewayId)).orElse(null);
        if (existing != null) {
            return existing;
        }

        McpGateway gateway = new McpGateway();
        gateway.setGatewayId(resolvedGatewayId);
        gateway.setGatewayName("默认网关");
        gateway.setGatewayDesc("系统自动创建的默认网关实例");
        gateway.setGatewayVersion("1.0.0");
        gateway.setGatewayInstructions("单网关模式默认实例");
        gateway.setStatus(1);
        gateway.setCreatedAt(LocalDateTime.now());
        gateway.setUpdatedAt(LocalDateTime.now());
        try {
            return gatewayRepository.save(gateway);
        } catch (Exception e) {
            return gatewayRepository.findByGatewayId(new GatewayIdQuery(resolvedGatewayId)).orElse(gateway);
        }
    }

    private Result<Void> updateGatewayAuthStatus(IdQuery query, int status) {
        if (query == null || query.getId() == null) {
            return Result.error("ID 不能为空");
        }
        McpGatewayAuth auth = gatewayAuthRepository.findById(query.getId()).orElse(null);
        if (auth == null) {
            return Result.error("凭证不存在");
        }
        auth.setStatus(status);
        auth.setUpdatedAt(LocalDateTime.now());
        gatewayAuthRepository.save(auth);
        return Result.success();
    }

    private int resolveRateLimit(Integer rateLimit) {
        if (rateLimit == null || rateLimit <= 0) {
            return 100;
        }
        return rateLimit;
    }

    private int resolveStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            return 1;
        }
        return status;
    }

    private String resolveApiKey(String inputApiKey, String existingApiKey, boolean createMode) {
        if (StringUtils.hasText(inputApiKey)) {
            return inputApiKey.trim();
        }
        if (StringUtils.hasText(existingApiKey)) {
            return existingApiKey.trim();
        }
        if (!createMode) {
            return null;
        }
        return "gk_" + UUID.randomUUID().toString().replace("-", "");
    }

    @Data
    public static class GatewayInstanceRequest {

        private Long id;
        private String gatewayId;
        private String gatewayName;
        private String gatewayDesc;
        private String gatewayVersion;
        private String gatewayInstructions;
        private Integer status;
    }

    @Data
    public static class ToolListRequest {

        private String gatewayId;
        private Integer pageNum;
        private Integer pageSize;
    }

    @Data
    public static class GatewayAuthListRequest {

        private String gatewayId;
        private String apiKeyKeyword;
        private Integer status;
        private Integer pageNum;
        private Integer pageSize;
    }

    @Data
    public static class SaveGatewayAuthRequest {

        private Long id;
        private String gatewayId;
        private String apiKey;
        private Integer rateLimit;
        private LocalDateTime expireTime;
        private Integer status;
    }

    @Data
    public static class SaveToolRequest {

        private Long id;
        private String gatewayId;
        private String toolName;
        private String toolDescription;
        private String httpUrl;
        private String httpMethod;
        private String httpHeaders;
        private Integer timeout;
        private Integer retryTimes;
        private Integer status;
        private List<MappingNodeRequest> requestMappings;
        private List<MappingNodeRequest> responseMappings;
    }

    @Data
    public static class MappingNodeRequest {

        private Long parentId;
        private String fieldName;
        private String mcpType;
        private String mcpDesc;
        private Boolean isRequired;
        private String itemType;
        private Long itemRefId;
        private String httpPath;
        private String httpLocation;
        private Integer sortOrder;
        private List<MappingNodeRequest> children;
    }

    @Data
    public static class ToolDebugRequest {

        private String gatewayId;
        private String toolName;
        private Map<String, Object> arguments;
    }

    @Data
    public static class ModelBindingQueryRequest {

        private Long modelId;
    }

    @Data
    public static class SaveModelBindingRequest {

        private Long modelId;
        private List<Long> toolIds;
    }

    @Data
    public static class GatewayMetricsQueryRequest {

        private String gatewayId;
        private String toolName;
        private Integer recentMinutes;
    }
}
