package com.xbk.knowledge.infrastructure.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.context.GatewayToolBindingContextHolder;
import com.xbk.knowledge.application.context.GatewayToolBindingContextHolder.BindingContext;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentRunRepository;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentRunContextRepository;
import com.xbk.knowledge.domain.agent.model.entity.AgentRun;
import com.xbk.knowledge.domain.agent.model.entity.AgentRunContext;
import com.xbk.knowledge.domain.approval.model.entity.ApprovalRequest;
import com.xbk.knowledge.domain.approval.adapter.repository.ApprovalRequestRepository;
import com.xbk.knowledge.domain.gateway.model.entity.McpGateway;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolBinding;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolRegistry;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayIdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolBindingQuery;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpGatewayRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolBindingRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolRegistryRepository;
import com.xbk.knowledge.domain.gateway.service.GatewayToolService;
import com.xbk.knowledge.infrastructure.audit.IdentityAuditLogService;
import com.xbk.knowledge.domain.audit.model.entity.SysAuditEvent;
import com.xbk.knowledge.types.exception.ApprovalRequiredException;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.json.JsonMapUtils;
import com.xbk.knowledge.types.tool.ToolNameUtils;
import com.xbk.knowledge.types.tool.ToolKeyAware;
import com.xbk.knowledge.types.tool.ToolInvokeBypassContextHolder;
import com.xbk.knowledge.types.trace.TraceIdUtils;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;

/**
 * Gateway ToolCallback 提供者
 * <p>
 * 职责：将 Gateway 配置的 HTTP 工具转换为 Spring AI 的 ToolCallback，
 * 注入到 AI 模型调用链路中。支持工具绑定过滤（按模型/话维度控制工具可见性）
 * <p>
 * 需要Gateway 工具以 HTTP 配置形式存储在数据库中，需要适配为 Spring AI
 * 的 FunctionToolCallback 才能被 ChatClient 识别和调用
 *
 * @author sxie
 */
@Slf4j
@Component("gatewayToolCallbackProvider")
@RequiredArgsConstructor
public class GatewayToolCallbackProvider implements ToolCallbackProvider {
    /**
     * MDC 中网关工具调用 ID 的键名。
     */
    private static final String CALL_ID_MDC_KEY = "gatewayToolCallId";

    /**
     * 工具绑定类型模型级绑定。
     */
    private static final String BIND_TYPE_MODEL = "MODEL";

    /**
     * 工具绑定类型会话级绑定。
     */
    private static final String BIND_TYPE_SESSION = "SESSION";

    /**
     * 工具绑定类型Agent 版本级绑定。
     */
    private static final String BIND_TYPE_AGENT_VERSION = "AGENT_VERSION";

    /**
     * 工具调用权限编码。
     */
    private static final String PERMISSION_TOOL_INVOKE = "tool:invoke";

    /**
     * 网关仓储，用于加载启用网关列表。
     */
    private final McpGatewayRepository gatewayRepository;

    /**
     * 工具注册仓储，用于读取网关下工具注册信息。
     */
    private final McpToolRegistryRepository toolRegistryRepository;

    /**
     * 工具绑定仓储，用于按模型/话/版本过滤工具可见性。
     */
    private final McpToolBindingRepository toolBindingRepository;

    /**
     * 网关工具服务，用于查询工具定义与发起网关调用。
     */
    private final GatewayToolService gatewayToolService;

    /**
     * JSON 序列化组件，用于处理工具参数与日志序列化。
     */
    private final ObjectMapper objectMapper;

    /**
     * Agent 运行记录仓储，用于更新工具调用计数。
     */
    private final AgentRunRepository agentRunRepository;

    /**
     * Agent 运行上下文仓储，用于读取审批和运行上下文。
     */
    private final AgentRunContextRepository agentRunContextRepository;

    /**
     * 身份审计日志服务，用于记录工具调用审计事件。
     */
    private final IdentityAuditLogService auditLogService;

    /**
     * 审批单仓储，用于高风险工具审批查询与创建。
     */
    private final ApprovalRequestRepository approvalRequestRepository;

    /**
     * 获取所有可用的 Gateway 工具回调
     * 流程：加载已启用网关 → 获取工具定义 → 应用绑定过滤 → 去重 → 构建 FunctionToolCallback
     *
     * @return 可用工具回调数组。
     */
    @Override
    public ToolCallback[] getToolCallbacks() {
        // 查询所有启用中的网关，无网关时直接返回空工具集。
        List<McpGateway> enabledGateways = gatewayRepository.findAllEnabled();
        if (enabledGateways == null || enabledGateways.isEmpty()) {
            return new ToolCallback[0];
        }

        // 先批量加载各网关的工具定义，后续按网关和工具名快速匹配。
        List<ToolCandidate> allCandidates = new ArrayList<>();
        Map<String, Map<String, GatewayToolService.ToolDefinition>> toolDefinitionByGateway = loadToolDefinitions(enabledGateways);

        // 遍历启用网关，组装原始工具候选列表。
        for (McpGateway gateway : enabledGateways) {
            // 网关对象或 gatewayId 无效时跳过。
            if (gateway == null || !StringUtils.hasText(gateway.getGatewayId())) {
                continue;
            }
            // 仅加载当前网关下启用状态的工具注册项。
            List<McpToolRegistry> toolRegistries = toolRegistryRepository.findEnabledByGatewayId(
                    new GatewayIdQuery(gateway.getGatewayId())
            );
            if (toolRegistries == null || toolRegistries.isEmpty()) {
                continue;
            }
            // 取当前网关的工具定义映射；未命中时降级为空映射避免空指针。
            Map<String, GatewayToolService.ToolDefinition> toolDefinitionMap = toolDefinitionByGateway.getOrDefault(
                    gateway.getGatewayId(),
                    Collections.emptyMap()
            );
            for (McpToolRegistry registry : toolRegistries) {
                // 注册记录缺关键字段时跳过。
                if (registry == null || registry.getId() == null || !StringUtils.hasText(registry.getToolName())) {
                    continue;
                }
                // 按工具名关联定义并构建候选对象。
                GatewayToolService.ToolDefinition definition = toolDefinitionMap.get(registry.getToolName());
                ToolCandidate toolCandidate = buildToolCandidate(gateway.getGatewayId(), registry, definition);
                allCandidates.add(toolCandidate);
            }
        }

        // 没有候选工具时直接返回空结果。
        if (allCandidates.isEmpty()) {
            return new ToolCallback[0];
        }

        // 套用上下文可见性规则（allowlist/绑定关系）后再继续构建。
        List<ToolCandidate> visibleCandidates = applyVisibilityFilter(allCandidates);
        if (visibleCandidates.isEmpty()) {
            return new ToolCallback[0];
        }

        // 构建最终 ToolCallback，并按 functionName 去重。
        List<ToolCallback> callbacks = new ArrayList<>();
        Set<String> dedupNames = new HashSet<>();
        for (ToolCandidate candidate : visibleCandidates) {
            // 同名函数只保留首个，后续重复项告警并忽略。
            if (!dedupNames.add(candidate.functionName)) {
                log.warn("发现重名 gateway 工具，已跳过后续重复项: functionName={}, toolKey={}",
                        candidate.functionName, candidate.toolKey);
                continue;
            }
            // 候选对象转换为可执行回调。
            callbacks.add(buildToolCallback(candidate));
        }

        // 输出本次返回规模，便于排查工具可见性问题。
        log.info("GatewayToolCallbackProvider 返回工具数量: {}", callbacks.size());
        return callbacks.toArray(new ToolCallback[0]);
    }

    /**
     * 批量加载所有网关的工具定义，返回 gatewayId → (toolName → ToolDefinition) 映射
     *
     * @param gateways 网关列表。
     */
    private Map<String, Map<String, GatewayToolService.ToolDefinition>> loadToolDefinitions(List<McpGateway> gateways) {
        // 结果结构：gatewayId -> (toolName -> ToolDefinition)
        Map<String, Map<String, GatewayToolService.ToolDefinition>> result = new HashMap<>();
        for (McpGateway gateway : gateways) {
            // 过滤无效网关，避免空指针和无意义查询。
            if (gateway == null || !StringUtils.hasText(gateway.getGatewayId())) {
                continue;
            }
            try {
                // 逐网关拉取工具定义；单个网关失败不影响其他网关。
                List<GatewayToolService.ToolDefinition> definitions = gatewayToolService.listTools(gateway.getGatewayId());
                // 当前网关内按 toolName 建索引，便于后续 O(1) 查找。
                Map<String, GatewayToolService.ToolDefinition> map = new HashMap<>();
                for (GatewayToolService.ToolDefinition definition : definitions) {
                    // 跳过无效定义（空对象或无工具名）。
                    if (definition == null || !StringUtils.hasText(definition.name())) {
                        continue;
                    }
                    map.put(definition.name(), definition);
                }
                // 即使该网关没有有效工具，也保留空 map 以表达“已加载该网关”。
                result.put(gateway.getGatewayId(), map);
            } catch (Exception e) {
                // 容错策略：记录告警并继续处理后续网关，避免整体失败。
                log.warn("加载 gateway 工具定义失败，gatewayId: {}", gateway.getGatewayId(), e);
            }
        }
        return result;
    }

    /**
     * 根据 ThreadLocal 中的绑定上下文过滤工具候选列表（无上下文时不过滤）。
     * <p>
     * 过滤顺序
     * 1、 allowlist（toolKey）优先且强制（当上下文显式携带 allowedToolKeys 时）
     * 2、 legacy 绑定过滤（MODEL/SESSION/AGENT_VERSION），当存在绑定记录时生效
     *
     * @param candidates 候选工具列表。
     * @return ToolCandidate 列表。
     */
    private List<ToolCandidate> applyVisibilityFilter(List<ToolCandidate> candidates) {
        // 无绑定上下文时，保持全量候选可见（兼容历史行为）。
        BindingContext context = GatewayToolBindingContextHolder.get();
        if (context == null) {
            return candidates;
        }

        // 第一步：先执行 allowlist（toolKey 维度），这是最强约束。
        List<ToolCandidate> afterAllowlist = applyAllowlistIfPresent(candidates, context);
        if (afterAllowlist.isEmpty()) {
            // allowlist 命中空集时直接返回，避免继续查绑定表。
            return Collections.emptyList();
        }

        // 第二步：收集历史绑定规则（model/session/agentVersion）对应的可见工具 ID。
        Set<Long> boundToolIds = new HashSet<>();
        boolean hasBinding = false;

        // 1、MODEL 维度绑定：将“该模型”允许的工具并入可见集合（并集）。
        Long modelId = context.getModelId();
        if (modelId != null) {
            List<McpToolBinding> modelBindings = toolBindingRepository.findByBindTypeAndTargetId(
                    new ToolBindingQuery(BIND_TYPE_MODEL, modelId)
            );
            if (modelBindings != null && !modelBindings.isEmpty()) {
                // 只要命中一条绑定记录，即进入“按绑定收敛可见工具”模式；
                // 若记录全部为禁用项，最终可见集合可能为空。
                hasBinding = true;
                // 仅追加 enabled=true（或空视为启用）的工具 ID。
                appendEnabledToolIds(boundToolIds, modelBindings);
            }
        }

        // 2、SESSION 维度绑定：将“当前会话”允许的工具并入可见集合（并集）。
        Long sessionId = context.getSessionId();
        if (sessionId != null) {
            List<McpToolBinding> sessionBindings = toolBindingRepository.findByBindTypeAndTargetId(
                    new ToolBindingQuery(BIND_TYPE_SESSION, sessionId)
            );
            if (sessionBindings != null && !sessionBindings.isEmpty()) {
                hasBinding = true;
                appendEnabledToolIds(boundToolIds, sessionBindings);
            }
        }

        // 3、AGENT_VERSION 维度绑定：将“当前 Agent 版本”允许的工具并入可见集合（并集）。
        Long agentVersionId = context.getAgentVersionId();
        if (agentVersionId != null) {
            List<McpToolBinding> bindings = toolBindingRepository.findByBindTypeAndTargetId(
                    new ToolBindingQuery(BIND_TYPE_AGENT_VERSION, agentVersionId)
            );
            if (bindings != null && !bindings.isEmpty()) {
                hasBinding = true;
                appendEnabledToolIds(boundToolIds, bindings);
            }
        }

        // 若上下文未配置任何历史绑定规则，则沿用 allowlist 过滤结果。
        if (!hasBinding) {
            return afterAllowlist;
        }

        // 存在 legacy 绑定时，仅保留命中绑定工具 ID 的候选项。
        List<ToolCandidate> filtered = new ArrayList<>();
        for (ToolCandidate candidate : afterAllowlist) {
            if (boundToolIds.contains(candidate.toolId)) {
                filtered.add(candidate);
            }
        }
        return filtered;
    }

    /**
     * 按 allowlist 规则过滤工具候选列表。
     *
     * @param candidates 候选工具列表。
     * @param context    执行上下文。
     * @return 过滤后的候选工具列表。
     */
    private List<ToolCandidate> applyAllowlistIfPresent(List<ToolCandidate> candidates,
                                                        BindingContext context) {
        if (context == null) {
            return candidates;
        }
        Set<String> allowedToolKeys = context.getAllowedToolKeys();
        if (allowedToolKeys == null) {
            return candidates;
        }
        if (allowedToolKeys.isEmpty()) {
            return Collections.emptyList();
        }
        List<ToolCandidate> filtered = new ArrayList<>();
        for (ToolCandidate candidate : candidates) {
            if (candidate == null || !StringUtils.hasText(candidate.toolKey)) {
                continue;
            }
            if (allowedToolKeys.contains(candidate.toolKey)) {
                filtered.add(candidate);
            }
        }
        return filtered;
    }

    /**
     * 从绑定列表中收集已启用的工具 ID
     *
     * @param collector 工具 ID 收集器。
     * @param bindings  工具绑定列表。
     */
    private void appendEnabledToolIds(Set<Long> collector, List<McpToolBinding> bindings) {
        for (McpToolBinding binding : bindings) {
            if (binding == null || binding.getToolId() == null) {
                continue;
            }
            if (Boolean.FALSE.equals(binding.getEnabled())) {
                continue;
            }
            collector.add(binding.getToolId());
        }
    }

    /**
     * 将工具注册信息和定义组装为候选对象
     *
     * @param gatewayId  标识 ID。
     * @param registry   工具注册配置。
     * @param definition 工具定义。
     * @return 工具候选数据。
     */
    private ToolCandidate buildToolCandidate(String gatewayId,
                                             McpToolRegistry registry,
                                             GatewayToolService.ToolDefinition definition) {
        // 将结构化 inputSchema 转成 JSON 字符串，给 FunctionToolCallback 注册使用。
        String inputSchema = "";
        if (definition != null && definition.inputSchema() != null && !definition.inputSchema().isEmpty()) {
            try {
                inputSchema = objectMapper.writeValueAsString(definition.inputSchema());
            } catch (Exception e) {
                log.warn("Gateway 工具 inputSchema 序列化失败，toolName: {}", registry.getToolName(), e);
            }
        }

        String description = StringUtils.hasText(registry.getToolDescription())
                ? registry.getToolDescription()
                : "";

        // toolKey 为空时按约定生成兜底键，确保后续审计和绑定可定位到唯一工具。
        String toolKey = registry.getToolKey();
        if (!StringUtils.hasText(toolKey) && StringUtils.hasText(gatewayId) && StringUtils.hasText(registry.getToolName())) {
            toolKey = "gateway:" + gatewayId + ":" + registry.getToolName();
        }
        // functionName 用于注册给模型侧的函数调用名，需要做安全字符规范化。
        String functionName = ToolNameUtils.safeFunctionName("gw", gatewayId, registry.getToolName());
        // riskLevel 缺省回落 MEDIUM，供审批/治理链路判断是否触发高风险门禁。
        String riskLevel = StringUtils.hasText(registry.getRiskLevel()) ? registry.getRiskLevel() : "MEDIUM";
        return new ToolCandidate(registry.getId(), gatewayId, registry.getToolName(), toolKey, functionName, description, inputSchema, riskLevel);
    }

    /**
     * 将候选对象构建为 FunctionToolCallback，内部封装工具调用逻辑和链路追踪
     *
     * @param candidate 工具候选数据。
     * @return 工具回调。
     */
    private ToolCallback buildToolCallback(ToolCandidate candidate) {
        String inputSchema = candidate.inputSchema;
        if (!StringUtils.hasText(inputSchema)) {
            inputSchema = "{\"type\":\"object\",\"properties\":{}}";
        }

        ToolMetadata metadata = ToolMetadata.builder()
                .returnDirect(false)
                .build();

        ToolCallback delegate = FunctionToolCallback
                .<Map<String, Object>, String>builder(candidate.functionName, arguments -> {
                    Map<String, Object> safeArgs = arguments == null ? Collections.emptyMap() : arguments;
                    String callId = UUID.randomUUID().toString().replace("-", "");
                    long startAt = System.nanoTime();
                    String previousCallId = MDC.get(CALL_ID_MDC_KEY);
                    MDC.put(CALL_ID_MDC_KEY, callId);
                    String runId = resolveRunId();
                    Long operatorId = resolveOperatorId();
                    boolean bypassEnabled = ToolInvokeBypassContextHolder.isEnabled();
                    boolean toolInvokePermitted = isToolInvokePermitted();
                    if (!bypassEnabled && !toolInvokePermitted) {
                        recordToolDeniedAndAudit(runId, candidate, "PERMISSION_DENIED", operatorId);
                        return "[PERMISSION_DENIED] 无权限调用工具（缺少权限: " + PERMISSION_TOOL_INVOKE + "），toolKey=" + candidate.toolKey;
                    }
                    String requesterType = operatorId == null ? "system" : "user";
                    maybeRequireApproval(runId, candidate, safeArgs, operatorId, requesterType, operatorId);
                    log.info("gateway_tool_call source=AI stage=start runId={} callId={} gatewayId={} toolName={} functionName={} toolKey={} argsKeys={}",
                            runId,
                            callId,
                            candidate.gatewayId,
                            candidate.toolName,
                            candidate.functionName,
                            candidate.toolKey,
                            safeArgs.keySet());

                    try {
                        GatewayToolService.ToolCallResult callResult = gatewayToolService.callTool(
                                candidate.gatewayId,
                                candidate.toolName,
                                safeArgs
                        );
                        long latencyMs = (System.nanoTime() - startAt) / 1_000_000;
                        log.info("gateway_tool_call source=AI stage=end runId={} callId={} gatewayId={} toolName={} functionName={} toolKey={} argsKeys={} success={} errorCode={} latencyMs={}",
                                runId,
                                callId,
                                candidate.gatewayId,
                                candidate.toolName,
                                candidate.functionName,
                                candidate.toolKey,
                                safeArgs.keySet(),
                                callResult.success(),
                                callResult.errorCode(),
                                latencyMs);

                        recordToolMetricsAndAudit(runId, candidate, safeArgs, callResult.success(), callResult.errorCode(), latencyMs, null, operatorId);

                        if (callResult.success()) {
                            return callResult.content();
                        }
                        String errorCode = callResult.errorCode() == null ? "TOOL_ERROR" : callResult.errorCode();
                        return "[" + errorCode.toUpperCase(Locale.ROOT) + "] " + callResult.content();
                    } catch (Exception e) {
                        long latencyMs = (System.nanoTime() - startAt) / 1_000_000;
                        log.error("gateway_tool_call source=AI stage=exception runId={} callId={} gatewayId={} toolName={} functionName={} toolKey={} argsKeys={} success=false errorCode={} latencyMs={} error={}",
                                runId,
                                callId,
                                candidate.gatewayId,
                                candidate.toolName,
                                candidate.functionName,
                                candidate.toolKey,
                                safeArgs.keySet(),
                                "TOOL_EXEC_FAILED",
                                latencyMs,
                                e.getMessage(),
                                e);
                        recordToolMetricsAndAudit(runId, candidate, safeArgs, false, "TOOL_EXEC_FAILED", latencyMs, e.getMessage(), operatorId);
                        throw e;
                    } finally {
                        if (StringUtils.hasText(previousCallId)) {
                            MDC.put(CALL_ID_MDC_KEY, previousCallId);
                        } else {
                            MDC.remove(CALL_ID_MDC_KEY);
                        }
                    }
                })
                .description(candidate.description)
                .inputType(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .inputSchema(inputSchema)
                .toolMetadata(metadata)
                .build();
        return new GovernedToolCallback(delegate, candidate.toolKey, "GATEWAY");
    }

    private String resolveRunId() {
        BindingContext context = GatewayToolBindingContextHolder.get();
        if (context != null && StringUtils.hasText(context.getRunId())) {
            return context.getRunId();
        }
        return TraceIdUtils.getOrCreateTraceId();
    }

    private Long resolveOperatorId() {
        BindingContext context = GatewayToolBindingContextHolder.get();
        if (context != null && context.getOperatorId() != null) {
            return context.getOperatorId();
        }
        try {
            if (!StpUtil.isLogin()) {
                return null;
            }
            String loginId = StpUtil.getLoginIdAsString();
            if (!StringUtils.hasText(loginId)) {
                return null;
            }
            return Long.parseLong(loginId);
        } catch (Exception ignore) {
            return null;
        }
    }

    private boolean isToolInvokePermitted() {
        BindingContext context = GatewayToolBindingContextHolder.get();
        if (context != null && context.getToolInvokePermitted() != null) {
            return context.getToolInvokePermitted();
        }
        try {
            return !StpUtil.isLogin() || StpUtil.hasPermission(PERMISSION_TOOL_INVOKE);
        } catch (Exception ignore) {
            return false;
        }
    }

    /**
     * 高风险审批门禁（方式B）。
     * <p>
     * 规则：
     * 1、 风险等级取工具注册 risk_level，缺省 MEDIUM
     * 2、 riskLevel=HIGH 时触发审批
     * 3、 已存在 APPROVED 且未过期则放行；否则创建或复用 PENDING 审批单并中断执行
     *
     * @param runId         运行 ID。
     * @param candidate     工具候选数据。
     * @param safeArgs      已脱敏工具入参。
     * @param requesterId   标识 ID。
     * @param requesterType 请求者类型。
     * @param operatorId    操作人标识。
     */
    private void maybeRequireApproval(String runId,
                                      ToolCandidate candidate,
                                      Map<String, Object> safeArgs,
                                      Long requesterId,
                                      String requesterType,
                                      Long operatorId) {
        if (!StringUtils.hasText(runId) || candidate == null || !StringUtils.hasText(candidate.toolKey)) {
            return;
        }
        String riskLevel = StringUtils.hasText(candidate.riskLevel) ? candidate.riskLevel : "MEDIUM";
        if (!"HIGH".equalsIgnoreCase(riskLevel) || approvalRequestRepository == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        try {
            if (approvalRequestRepository.findLatestApproved(runId, candidate.toolKey, now).isPresent()) {
                return;
            }
        } catch (Exception e) {
            log.warn("查询 APPROVED 审批单失败，runId: {}, toolKey: {}", runId, candidate.toolKey, e);
        }

        try {
            ApprovalRequest pending = approvalRequestRepository
                    .findLatestPending(runId, candidate.toolKey, now)
                    .orElse(null);
            if (pending != null && pending.getId() != null) {
                throw new ApprovalRequiredException(
                        pending.getId(),
                        candidate.toolKey,
                        riskLevel,
                        "工具调用需要审批（已存在待审批单）"
                );
            }
        } catch (ApprovalRequiredException e) {
            throw e;
        } catch (Exception e) {
            log.warn("查询 PENDING 审批单失败，runId: {}, toolKey: {}", runId, candidate.toolKey, e);
        }

        String argsJson;
        try {
            argsJson = objectMapper.writeValueAsString(safeArgs == null ? Collections.emptyMap() : safeArgs);
        } catch (Exception e) {
            argsJson = "{}";
        }
        String digest = buildArgsDigest(safeArgs);

        Long agentId = null;
        Long agentVersionId = null;
        Long workflowId = null;
        Long workflowVersionId = null;
        String workflowNodeKey = null;

        // 优先识别 Workflow 归属（即使同时存在 agent_run，也按 workflow 审批续跑）
        BindingContext ctx = GatewayToolBindingContextHolder.get();
        workflowId = ctx == null ? null : ctx.getWorkflowId();
        workflowVersionId = ctx == null ? null : ctx.getWorkflowVersionId();
        workflowNodeKey = ctx == null ? null : ctx.getWorkflowNodeKey();
        if (workflowId == null || workflowVersionId == null || !StringUtils.hasText(workflowNodeKey)) {
            // fallback按 agent_run 归属
            AgentRun run = agentRunRepository
                    .findByRunId(runId)
                    .orElse(null);
            if (run != null && run.getAgentId() != null && run.getAgentVersionId() != null) {
                agentId = run.getAgentId();
                agentVersionId = run.getAgentVersionId();
            } else {
                throw new BusinessException("工具审批门禁触发，但无法定位运行归属（agent/workflow），runId=" + runId);
            }
        }

        ApprovalRequest req = ApprovalRequest.builder()
                .approvalType("TOOL_INVOKE")
                .status("PENDING")
                .runId(runId)
                .agentId(agentId)
                .agentVersionId(agentVersionId)
                .workflowId(workflowId)
                .workflowVersionId(workflowVersionId)
                .nodeKey(workflowNodeKey)
                .requesterId(requesterId)
                .requesterType(requesterType)
                .requestReason(null)
                .approverId(null)
                .decisionComment(null)
                .decidedAt(null)
                .toolKey(candidate.toolKey)
                .riskLevel(riskLevel.toUpperCase(Locale.ROOT))
                .argumentsSnapshotJson(argsJson)
                .argumentsDigest(digest)
                .expireAt(now.plusMinutes(30))
                .build();
        approvalRequestRepository.insert(req);
        if (agentId != null && agentVersionId != null) {
            upsertPendingToolSnapshot(runId, candidate.toolKey, riskLevel, digest, req.getId(), now);
        }
        recordApprovalCreatedAudit(runId, req, operatorId);
        throw new ApprovalRequiredException(req.getId(), candidate.toolKey, riskLevel, "工具调用需要审批（已生成审批单）");
    }

    /**
     * 更新待审批工具快照。
     *
     * @param runId             运行ID。
     * @param toolKey           工具标识。
     * @param riskLevel         风险级别。
     * @param argsDigest        参数摘要。
     * @param approvalRequestId 审批申请ID。
     * @param now               当前时间。
     */
    private void upsertPendingToolSnapshot(String runId,
                                           String toolKey,
                                           String riskLevel,
                                           String argsDigest,
                                           Long approvalRequestId,
                                           LocalDateTime now) {
        if (agentRunContextRepository == null || !StringUtils.hasText(runId)) {
            return;
        }
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            agentRunContextRepository.findByRunId(runId).ifPresent(ctx -> {
                if (ctx != null && StringUtils.hasText(ctx.getSnapshotJson())) {
                    try {
                        Map<String, Object> existed = JsonMapUtils.readMap(objectMapper, ctx.getSnapshotJson());
                        if (existed != null) {
                            map.putAll(existed);
                        }
                    } catch (Exception ignore) {
                        // 忽略旧快照解析失败，直接覆盖最小字段
                    }
                }
            });
            map.put("pendingToolKey", toolKey);
            map.put("pendingRiskLevel", riskLevel);
            map.put("pendingArgumentsDigest", argsDigest);
            map.put("approvalRequestId", approvalRequestId);
            map.put("pendingAt", now == null ? null : now.toString());

            String json = objectMapper.writeValueAsString(map);
            AgentRunContext ctx = AgentRunContext.builder()
                    .runId(runId)
                    .status("SAVED")
                    .snapshotJson(json)
                    .build();
            agentRunContextRepository.upsert(ctx);
        } catch (Exception e) {
            log.warn("更新 agent_run_context 待审批快照失败，runId: {}, toolKey: {}", runId, toolKey, e);
        }
    }

    private String buildArgsDigest(Map<String, Object> safeArgs) {
        if (safeArgs == null || safeArgs.isEmpty()) {
            return "argsKeys=[]";
        }
        return "argsKeys=" + safeArgs.keySet();
    }

    private void recordApprovalCreatedAudit(String runId, ApprovalRequest req, Long operatorId) {
        try {
            if (auditLogService == null || req == null || req.getId() == null) {
                return;
            }
            SysAuditEvent event = SysAuditEvent.builder()
                    .operatorId(operatorId)
                    .operatorType(operatorId == null ? "system" : "user")
                    .eventType("TOOL_APPROVAL")
                    .resourceType("approval_request")
                    .resourceId(String.valueOf(req.getId()))
                    .action("CREATED")
                    .requestId(runId)
                    .result(1)
                    .errorMessage(null)
                    .costMs(0L)
                    .newValue(null)
                    .oldValue(null)
                    .build();
            auditLogService.record(event);
        } catch (Exception e) {
            log.warn("写入审批创建审计失败，runId: {}, approvalId: {}", runId, req == null ? null : req.getId(), e);
        }
    }

    private void recordToolDeniedAndAudit(String runId,
                                          ToolCandidate candidate,
                                          String reason,
                                          Long operatorId) {
        try {
            if (agentRunRepository != null && StringUtils.hasText(runId)) {
                agentRunRepository.incrementToolDeniedCount(runId, 1);
            }
        } catch (Exception e) {
            log.warn("更新 agent_run 工具拒绝计数失败，runId: {}, toolKey: {}", runId, candidate == null ? null : candidate.toolKey, e);
        }

        try {
            if (auditLogService == null || candidate == null || !StringUtils.hasText(candidate.toolKey)) {
                return;
            }
            SysAuditEvent event = SysAuditEvent.builder()
                    .operatorId(operatorId)
                    .operatorType(operatorId == null ? "system" : "user")
                    .eventType("TOOL_INVOKE")
                    .resourceType("gateway_tool")
                    .resourceId(candidate.toolKey)
                    .action("DENIED")
                    .requestId(runId)
                    .result(0)
                    .errorMessage(reason)
                    .costMs(0L)
                    .build();
            auditLogService.record(event);
        } catch (Exception e) {
            log.warn("写入工具拒绝审计失败，runId: {}, toolKey: {}", runId, candidate == null ? null : candidate.toolKey, e);
        }
    }

    private void recordToolMetricsAndAudit(String runId,
                                           ToolCandidate candidate,
                                           Map<String, Object> safeArgs,
                                           boolean success,
                                           String errorCode,
                                           long latencyMs,
                                           String errorMessage,
                                           Long operatorId) {
        try {
            if (agentRunRepository != null && StringUtils.hasText(runId)) {
                agentRunRepository.incrementToolCallCount(runId, 1);
            }
        } catch (Exception e) {
            log.warn("更新 agent_run 工具调用计数失败，runId: {}, toolKey: {}", runId, candidate == null ? null : candidate.toolKey, e);
        }

        try {
            if (auditLogService == null || candidate == null || !StringUtils.hasText(candidate.toolKey)) {
                return;
            }
            SysAuditEvent event = SysAuditEvent.builder()
                    .operatorId(operatorId)
                    .operatorType(operatorId == null ? "system" : "user")
                    .eventType("TOOL_INVOKE")
                    .resourceType("gateway_tool")
                    .resourceId(candidate.toolKey)
                    .action(success ? "SUCCESS" : "FAILED")
                    .requestId(runId)
                    .result(success ? 1 : 0)
                    .errorMessage(success ? null : (errorMessage == null ? errorCode : errorMessage))
                    .costMs(latencyMs)
                    .newValue(null)
                    .oldValue(null)
                    .build();
            auditLogService.record(event);
        } catch (Exception e) {
            log.warn("写入工具调用审计失败，runId: {}, toolKey: {}", runId, candidate == null ? null : candidate.toolKey, e);
        }
    }

    /**
     * 工具候选对象，承载构建 ToolCallback 所需的中间数据
     */
    private static class ToolCandidate {
        /**
         * 工具注册 ID。
         */
        private final Long toolId;

        /**
         * 所属网关 ID。
         */
        private final String gatewayId;

        /**
         * 网关原始工具名。
         */
        private final String toolName;

        /**
         * 平台治理用工具键（gatewayId:toolName）。
         */
        private final String toolKey;

        /**
         * 暴露给模型侧的函数名（冲突治理后）。
         */
        private final String functionName;

        /**
         * 工具描述信息。
         */
        private final String description;

        /**
         * 工具输入参数 JSON Schema。
         */
        private final String inputSchema;

        /**
         * 工具风险等级（LOW/MEDIUM/HIGH）。
         */
        private final String riskLevel;

        /**
         * 将输入数据转换为Candidate。
         *
         * @param toolId       工具ID。
         * @param gatewayId    网关ID。
         * @param toolName     工具名称。
         * @param toolKey      工具标识。
         * @param functionName 函数名称。
         * @param description  描述文本。
         * @param inputSchema  输入Schema定义。
         * @param riskLevel    风险级别。
         */
        private ToolCandidate(Long toolId,
                              String gatewayId,
                              String toolName,
                              String toolKey,
                              String functionName,
                              String description,
                              String inputSchema,
                              String riskLevel) {
            this.toolId = toolId;
            this.gatewayId = gatewayId;
            this.toolName = toolName;
            this.toolKey = toolKey;
            this.functionName = functionName;
            this.description = description;
            this.inputSchema = inputSchema;
            this.riskLevel = riskLevel;
        }
    }

    /**
     * ToolCallback 包装器暴露 toolKey，用于工具目录/治理。
     * <p>
     * 说明：治理字段不应依赖 ToolDefinition.name（因冲突治理而变化）。
     */
    private static final class GovernedToolCallback implements ToolCallback, ToolKeyAware {
        /**
         * 底层 ToolCallback 实现。
         */
        private final ToolCallback delegate;

        /**
         * 平台治理工具键。
         */
        private final String toolKey;

        /**
         * 工具来源标记（当前为 gateway）。
         */
        private final String toolSource;

        private GovernedToolCallback(ToolCallback delegate, String toolKey, String toolSource) {
            this.delegate = delegate;
            this.toolKey = toolKey;
            this.toolSource = toolSource;
        }

        /**
         * 获取底层工具定义。
         *
         * @return ToolDefinition 数据。
         */
        @Override
        public ToolDefinition getToolDefinition() {
            return delegate.getToolDefinition();
        }

        /**
         * 获取底层工具元数据。
         *
         * @return ToolMetadata 数据。
         */
        @Override
        public ToolMetadata getToolMetadata() {
            return delegate.getToolMetadata();
        }

        /**
         * 调用底层工具（无上下文）。
         *
         * @param toolInput 工具输入参数。
         * @return 工具执行结果文本。
         */
        @Override
        public String call(String toolInput) {
            return delegate.call(toolInput);
        }

        /**
         * 调用底层工具（带上下文）。
         *
         * @param toolInput   工具输入参数。
         * @param toolContext 工具上下文。
         * @return 工具执行结果文本。
         */
        @Override
        public String call(String toolInput, ToolContext toolContext) {
            BindingContext previousContext = GatewayToolBindingContextHolder.get();
            boolean contextBound = bindToolContext(toolContext);
            try {
                return delegate.call(toolInput, toolContext);
            } finally {
                if (contextBound) {
                    GatewayToolBindingContextHolder.set(previousContext);
                }
            }
        }

        /**
         * 返回治理后的工具标识。
         *
         * @return 工具唯一标识。
         */
        @Override
        public String toolKey() {
            return toolKey;
        }

        /**
         * 返回工具来源标识。
         *
         * @return 工具来源。
         */
        @Override
        public String toolSource() {
            return toolSource;
        }

        /**
         * 将 ToolContext 里的权限快照绑定到 ThreadLocal，解决工具回调跨线程上下文丢失问题。
         *
         * @param toolContext 工具上下文。
         * @return 是否已完成绑定。
         */
        private boolean bindToolContext(ToolContext toolContext) {
            if (toolContext == null || toolContext.getContext() == null || toolContext.getContext().isEmpty()) {
                return false;
            }
            Map<String, Object> contextMap = toolContext.getContext();
            String runId = asString(contextMap.get(GatewayToolBindingContextHolder.TOOL_CONTEXT_RUN_ID));
            Long operatorId = asLong(contextMap.get(GatewayToolBindingContextHolder.TOOL_CONTEXT_OPERATOR_ID));
            Boolean toolInvokePermitted = asBoolean(contextMap.get(GatewayToolBindingContextHolder.TOOL_CONTEXT_TOOL_INVOKE_PERMITTED));
            if (!StringUtils.hasText(runId) && operatorId == null && toolInvokePermitted == null) {
                return false;
            }
            GatewayToolBindingContextHolder.set(null, null, null, runId, null, operatorId, toolInvokePermitted);
            return true;
        }

        private String asString(Object value) {
            if (value == null) {
                return null;
            }
            String text = String.valueOf(value);
            return StringUtils.hasText(text) ? text : null;
        }

        private Long asLong(Object value) {
            if (value == null) {
                return null;
            }
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            String text = String.valueOf(value);
            if (!StringUtils.hasText(text)) {
                return null;
            }
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignore) {
                return null;
            }
        }

        private Boolean asBoolean(Object value) {
            if (value == null) {
                return null;
            }
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            String text = String.valueOf(value);
            if (!StringUtils.hasText(text)) {
                return null;
            }
            return Boolean.parseBoolean(text.trim());
        }
    }
}
