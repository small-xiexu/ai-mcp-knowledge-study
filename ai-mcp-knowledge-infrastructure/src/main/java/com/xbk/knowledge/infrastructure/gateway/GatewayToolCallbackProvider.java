package com.xbk.knowledge.infrastructure.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.context.GatewayToolBindingContextHolder;
import com.xbk.knowledge.domain.repository.agent.AgentRunRepository;
import com.xbk.knowledge.domain.repository.agent.AgentRunContextRepository;
import com.xbk.knowledge.domain.model.entity.approval.ApprovalRequest;
import com.xbk.knowledge.domain.repository.approval.ApprovalRequestRepository;
import com.xbk.knowledge.domain.repository.tool.ToolPolicyRepository;
import com.xbk.knowledge.domain.model.entity.gateway.McpGateway;
import com.xbk.knowledge.domain.model.entity.gateway.McpToolBinding;
import com.xbk.knowledge.domain.model.entity.gateway.McpToolRegistry;
import com.xbk.knowledge.domain.model.vo.gateway.ToolBindingQuery;
import com.xbk.knowledge.domain.repository.gateway.McpGatewayRepository;
import com.xbk.knowledge.domain.repository.gateway.McpToolBindingRepository;
import com.xbk.knowledge.domain.repository.gateway.McpToolRegistryRepository;
import com.xbk.knowledge.domain.service.gateway.GatewayToolService;
import com.xbk.knowledge.infrastructure.audit.IdentityAuditLogService;
import com.xbk.knowledge.domain.model.entity.SysAuditEvent;
import com.xbk.knowledge.types.context.OrgContext;
import com.xbk.knowledge.types.context.OrgContextHolder;
import com.xbk.knowledge.types.exception.ApprovalRequiredException;
import com.xbk.knowledge.types.exception.BusinessException;
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
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
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
 *
 * 职责：将 Gateway 配置的 HTTP 工具转换为 Spring AI 的 ToolCallback，
 * 注入到 AI 模型调用链路中。支持工具绑定过滤（按模型/会话维度控制工具可见性）
 *
 * 为什么需要：Gateway 工具以 HTTP 配置形式存储在数据库中，需要适配为 Spring AI
 * 的 FunctionToolCallback 才能被 ChatClient 识别和调用
 *
 * @author xiexu
 */
@Slf4j
@Component("gatewayToolCallbackProvider")
@RequiredArgsConstructor
public class GatewayToolCallbackProvider implements ToolCallbackProvider {

    private static final String CALL_ID_MDC_KEY = "gatewayToolCallId";
    private static final String BIND_TYPE_MODEL = "MODEL";
    private static final String BIND_TYPE_SESSION = "SESSION";
    private static final String BIND_TYPE_AGENT_VERSION = "AGENT_VERSION";
    private static final String PERMISSION_TOOL_INVOKE = "tool:invoke";

    private final McpGatewayRepository gatewayRepository;
    private final McpToolRegistryRepository toolRegistryRepository;
    private final McpToolBindingRepository toolBindingRepository;
    private final GatewayToolService gatewayToolService;
    private final ObjectMapper objectMapper;
    private final AgentRunRepository agentRunRepository;
    private final AgentRunContextRepository agentRunContextRepository;
    private final IdentityAuditLogService auditLogService;
    private final ToolPolicyRepository toolPolicyRepository;
    private final ApprovalRequestRepository approvalRequestRepository;

    /**
     * 获取所有可用的 Gateway 工具回调
     * 流程：加载已启用网关 → 获取工具定义 → 应用绑定过滤 → 去重 → 构建 FunctionToolCallback
     */
    @Override
    public ToolCallback[] getToolCallbacks() {
        List<McpGateway> enabledGateways = gatewayRepository.findAllEnabled();
        if (enabledGateways == null || enabledGateways.isEmpty()) {
            return new ToolCallback[0];
        }

        List<ToolCandidate> allCandidates = new ArrayList<>();
        Map<String, Map<String, GatewayToolService.ToolDefinition>> toolDefinitionByGateway = loadToolDefinitions(enabledGateways);

        for (McpGateway gateway : enabledGateways) {
            if (gateway == null || !StringUtils.hasText(gateway.getGatewayId())) {
                continue;
            }
            List<McpToolRegistry> toolRegistries = toolRegistryRepository.findEnabledByGatewayId(
                    new com.xbk.knowledge.domain.model.vo.gateway.GatewayIdQuery(gateway.getGatewayId())
            );
            if (toolRegistries == null || toolRegistries.isEmpty()) {
                continue;
            }
            Map<String, GatewayToolService.ToolDefinition> toolDefinitionMap = toolDefinitionByGateway.getOrDefault(
                    gateway.getGatewayId(),
                    Collections.emptyMap()
            );
            for (McpToolRegistry registry : toolRegistries) {
                if (registry == null || registry.getId() == null || !StringUtils.hasText(registry.getToolName())) {
                    continue;
                }
                GatewayToolService.ToolDefinition definition = toolDefinitionMap.get(registry.getToolName());
                allCandidates.add(buildToolCandidate(gateway.getGatewayId(), registry, definition));
            }
        }

        if (allCandidates.isEmpty()) {
            return new ToolCallback[0];
        }

        List<ToolCandidate> visibleCandidates = applyVisibilityFilter(allCandidates);
        if (visibleCandidates.isEmpty()) {
            return new ToolCallback[0];
        }

        List<ToolCallback> callbacks = new ArrayList<>();
        Set<String> dedupNames = new HashSet<>();
        for (ToolCandidate candidate : visibleCandidates) {
            if (!dedupNames.add(candidate.functionName)) {
                log.warn("发现重名 gateway 工具，已跳过后续重复项: functionName={}, toolKey={}",
                        candidate.functionName, candidate.toolKey);
                continue;
            }
            callbacks.add(buildToolCallback(candidate));
        }

        log.info("GatewayToolCallbackProvider 返回工具数量: {}", callbacks.size());
        return callbacks.toArray(new ToolCallback[0]);
    }

    /** 批量加载所有网关的工具定义，返回 gatewayId → (toolName → ToolDefinition) 映射 */
    private Map<String, Map<String, GatewayToolService.ToolDefinition>> loadToolDefinitions(List<McpGateway> gateways) {
        Map<String, Map<String, GatewayToolService.ToolDefinition>> result = new HashMap<>();
        for (McpGateway gateway : gateways) {
            if (gateway == null || !StringUtils.hasText(gateway.getGatewayId())) {
                continue;
            }
            try {
                List<GatewayToolService.ToolDefinition> definitions = gatewayToolService.listTools(gateway.getGatewayId());
                Map<String, GatewayToolService.ToolDefinition> map = new HashMap<>();
                for (GatewayToolService.ToolDefinition definition : definitions) {
                    if (definition == null || !StringUtils.hasText(definition.name())) {
                        continue;
                    }
                    map.put(definition.name(), definition);
                }
                result.put(gateway.getGatewayId(), map);
            } catch (Exception e) {
                log.warn("加载 gateway 工具定义失败，gatewayId: {}", gateway.getGatewayId(), e);
            }
        }
        return result;
    }

    /**
     * 根据 ThreadLocal 中的绑定上下文过滤工具候选列表（无上下文时不过滤）。
     *
     * 过滤顺序：
     * 1) allowlist（toolKey）优先且强制（当上下文显式携带 allowedToolKeys 时）
     * 2) legacy 绑定过滤（MODEL/SESSION/AGENT_VERSION），当存在绑定记录时生效
     */
    private List<ToolCandidate> applyVisibilityFilter(List<ToolCandidate> candidates) {
        GatewayToolBindingContextHolder.BindingContext context = GatewayToolBindingContextHolder.get();
        if (context == null) {
            return candidates;
        }

        List<ToolCandidate> afterAllowlist = applyAllowlistIfPresent(candidates, context);
        if (afterAllowlist.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> boundToolIds = new HashSet<>();
        boolean hasBinding = false;

        Long modelId = context.getModelId();
        if (modelId != null) {
            List<McpToolBinding> modelBindings = toolBindingRepository.findByBindTypeAndTargetId(
                    new ToolBindingQuery(BIND_TYPE_MODEL, modelId)
            );
            if (modelBindings != null && !modelBindings.isEmpty()) {
                hasBinding = true;
                appendEnabledToolIds(boundToolIds, modelBindings);
            }
        }

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

        if (!hasBinding) {
            return afterAllowlist;
        }

        List<ToolCandidate> filtered = new ArrayList<>();
        for (ToolCandidate candidate : afterAllowlist) {
            if (boundToolIds.contains(candidate.toolId)) {
                filtered.add(candidate);
            }
        }
        return filtered;
    }

    private List<ToolCandidate> applyAllowlistIfPresent(List<ToolCandidate> candidates,
                                                       GatewayToolBindingContextHolder.BindingContext context) {
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

    /** 从绑定列表中收集已启用的工具 ID */
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

    /** 将工具注册信息和定义组装为候选对象 */
    private ToolCandidate buildToolCandidate(String gatewayId,
                                             McpToolRegistry registry,
                                             GatewayToolService.ToolDefinition definition) {
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

        String toolKey = registry.getToolKey();
        if (!StringUtils.hasText(toolKey) && StringUtils.hasText(gatewayId) && StringUtils.hasText(registry.getToolName())) {
            toolKey = "gateway:" + gatewayId + ":" + registry.getToolName();
        }
        String functionName = ToolNameUtils.safeFunctionName("gw", gatewayId, registry.getToolName());
        String riskLevel = StringUtils.hasText(registry.getRiskLevel()) ? registry.getRiskLevel() : "MEDIUM";
        return new ToolCandidate(registry.getId(), gatewayId, registry.getToolName(), toolKey, functionName, description, inputSchema, riskLevel);
    }

    /** 将候选对象构建为 FunctionToolCallback，内部封装工具调用逻辑和链路追踪 */
    private ToolCallback buildToolCallback(ToolCandidate candidate) {
        String inputSchema = candidate.inputSchema;
        if (!StringUtils.hasText(inputSchema)) {
            inputSchema = "{\"type\":\"object\",\"properties\":{}}";
        }

        /*
         * 重要：工具回调可能在异步/跨线程环境执行（例如流式/Reactive）。
         * 为保证治理门禁一致性（orgId、runId、权限、审计归属），在构建回调时捕获上下文快照，
         * call() 期间禁止再依赖 ThreadLocal（OrgContextHolder/StpUtil/MDC）获取关键字段。
         */
        final String boundRunId = TraceIdUtils.getOrCreateTraceId();
        Long boundOrgId = OrgContextHolder.currentOrgIdOrNull();
        if (boundOrgId == null) {
            boundOrgId = 1L;
        }
        final Long finalOrgId = boundOrgId;
        OrgContext orgCtx = OrgContextHolder.get();
        final Long boundOperatorId = orgCtx == null ? null : orgCtx.operatorUserId();
        final Long boundOperatorOrgId = orgCtx == null ? null : orgCtx.operatorOrgId();
        final boolean bypassEnabled = ToolInvokeBypassContextHolder.isEnabled();
        final boolean login = StpUtil.isLogin();
        final boolean toolInvokePermitted = !login || StpUtil.hasPermission(PERMISSION_TOOL_INVOKE);

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
                    String runId = boundRunId;
                    Long orgId = finalOrgId;
                    if (!bypassEnabled && !toolInvokePermitted) {
                        recordToolDeniedAndAudit(runId, orgId, candidate, "PERMISSION_DENIED", boundOperatorId, boundOperatorOrgId);
                        return "[PERMISSION_DENIED] 无权限调用工具（缺少权限: " + PERMISSION_TOOL_INVOKE + "），toolKey=" + candidate.toolKey;
                    }
                    String requesterType = boundOperatorId == null ? "system" : "user";
                    maybeRequireApproval(runId, orgId, candidate, safeArgs, boundOperatorId, requesterType, boundOperatorId, boundOperatorOrgId);
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

                        recordToolMetricsAndAudit(runId, orgId, candidate, safeArgs, callResult.success(), callResult.errorCode(), latencyMs, null, boundOperatorId, boundOperatorOrgId);

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
                        recordToolMetricsAndAudit(runId, orgId, candidate, safeArgs, false, "TOOL_EXEC_FAILED", latencyMs, e.getMessage(), boundOperatorId, boundOperatorOrgId);
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

    /**
     * 高风险审批门禁（方式B）。
     *
     * 规则：
     * 1) 风险等级来源优先 tool_policy（按 org + toolKey），其次取工具注册 risk_level，缺省 MEDIUM
     * 2) 当 tool_policy.approval_required=1 或 riskLevel=HIGH 时触发审批
     * 3) 已存在 APPROVED 且未过期则放行；否则创建或复用 PENDING 审批单并中断执行
     */
    private void maybeRequireApproval(String runId,
                                      Long orgId,
                                      ToolCandidate candidate,
                                      Map<String, Object> safeArgs,
                                      Long requesterId,
                                      String requesterType,
                                      Long operatorId,
                                      Long operatorOrgId) {
        if (!StringUtils.hasText(runId) || orgId == null || candidate == null || !StringUtils.hasText(candidate.toolKey)) {
            return;
        }
        String riskLevel = StringUtils.hasText(candidate.riskLevel) ? candidate.riskLevel : "MEDIUM";
        boolean approvalRequired = "HIGH".equalsIgnoreCase(riskLevel);

        // 重新基于 policy 计算（避免 lambda final 限制：拆成显式查询）
        try {
            if (toolPolicyRepository != null) {
                com.xbk.knowledge.domain.model.entity.tool.ToolPolicy policy =
                        toolPolicyRepository.findEnabled(orgId, candidate.toolKey).orElse(null);
                if (policy != null) {
                    if (StringUtils.hasText(policy.getRiskLevel())) {
                        riskLevel = policy.getRiskLevel();
                    }
                    if (policy.getApprovalRequired() != null && policy.getApprovalRequired() == 1) {
                        approvalRequired = true;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("查询 tool_policy 失败（第二次兜底），orgId: {}, toolKey: {}", orgId, candidate.toolKey, e);
        }

        riskLevel = StringUtils.hasText(riskLevel) ? riskLevel : "MEDIUM";
        if (!approvalRequired && "HIGH".equalsIgnoreCase(riskLevel)) {
            approvalRequired = true;
        }
        if (!approvalRequired || approvalRequestRepository == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        try {
            if (approvalRequestRepository.findLatestApproved(orgId, runId, candidate.toolKey, now).isPresent()) {
                return;
            }
        } catch (Exception e) {
            log.warn("查询 APPROVED 审批单失败，orgId: {}, runId: {}, toolKey: {}", orgId, runId, candidate.toolKey, e);
        }

        try {
            ApprovalRequest pending = approvalRequestRepository
                    .findLatestPending(orgId, runId, candidate.toolKey, now)
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
            log.warn("查询 PENDING 审批单失败，orgId: {}, runId: {}, toolKey: {}", orgId, runId, candidate.toolKey, e);
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
        com.xbk.knowledge.application.context.GatewayToolBindingContextHolder.BindingContext ctx =
                com.xbk.knowledge.application.context.GatewayToolBindingContextHolder.get();
        workflowId = ctx == null ? null : ctx.getWorkflowId();
        workflowVersionId = ctx == null ? null : ctx.getWorkflowVersionId();
        workflowNodeKey = ctx == null ? null : ctx.getWorkflowNodeKey();
        if (workflowId == null || workflowVersionId == null || !StringUtils.hasText(workflowNodeKey)) {
            // fallback：按 agent_run 归属
            com.xbk.knowledge.domain.model.entity.agent.AgentRun run = agentRunRepository
                    .findByRunId(orgId, runId)
                    .orElse(null);
            if (run != null && run.getAgentId() != null && run.getAgentVersionId() != null) {
                agentId = run.getAgentId();
                agentVersionId = run.getAgentVersionId();
            } else {
                throw new BusinessException("工具审批门禁触发，但无法定位运行归属（agent/workflow），runId=" + runId);
            }
        }

        ApprovalRequest req = ApprovalRequest.builder()
                .orgId(orgId)
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
            upsertPendingToolSnapshot(orgId, runId, candidate.toolKey, riskLevel, digest, req.getId(), now);
        }
        recordApprovalCreatedAudit(runId, orgId, req, operatorId, operatorOrgId);
        throw new ApprovalRequiredException(req.getId(), candidate.toolKey, riskLevel, "工具调用需要审批（已生成审批单）");
    }

    private void upsertPendingToolSnapshot(Long orgId,
                                          String runId,
                                          String toolKey,
                                          String riskLevel,
                                          String argsDigest,
                                          Long approvalRequestId,
                                          LocalDateTime now) {
        if (agentRunContextRepository == null || orgId == null || !StringUtils.hasText(runId)) {
            return;
        }
        try {
            java.util.Map<String, Object> map = new LinkedHashMap<>();
            agentRunContextRepository.findByRunId(orgId, runId).ifPresent(ctx -> {
                if (ctx != null && StringUtils.hasText(ctx.getSnapshotJson())) {
                    try {
                        java.util.Map<String, Object> existed = objectMapper.readValue(
                                ctx.getSnapshotJson(),
                                new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {}
                        );
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
            com.xbk.knowledge.domain.model.entity.agent.AgentRunContext ctx = com.xbk.knowledge.domain.model.entity.agent.AgentRunContext.builder()
                    .orgId(orgId)
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

    private void recordApprovalCreatedAudit(String runId, Long orgId, ApprovalRequest req, Long operatorId, Long operatorOrgId) {
        try {
            if (auditLogService == null || req == null || req.getId() == null) {
                return;
            }
            SysAuditEvent event = SysAuditEvent.builder()
                    .operatorId(operatorId)
                    .operatorOrgId(operatorOrgId)
                    .operatorType(operatorId == null ? "system" : "user")
                    .eventType("TOOL_APPROVAL")
                    .resourceType("approval_request")
                    .resourceId(String.valueOf(req.getId()))
                    .resourceOrgId(orgId)
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
                                          Long orgId,
                                          ToolCandidate candidate,
                                          String reason,
                                          Long operatorId,
                                          Long operatorOrgId) {
        try {
            if (agentRunRepository != null && StringUtils.hasText(runId)) {
                agentRunRepository.incrementToolDeniedCount(runId, orgId, 1);
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
                    .operatorOrgId(operatorOrgId)
                    .operatorType(operatorId == null ? "system" : "user")
                    .eventType("TOOL_INVOKE")
                    .resourceType("gateway_tool")
                    .resourceId(candidate.toolKey)
                    .resourceOrgId(orgId)
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
                                           Long orgId,
                                           ToolCandidate candidate,
                                           Map<String, Object> safeArgs,
                                           boolean success,
                                           String errorCode,
                                           long latencyMs,
                                           String errorMessage,
                                           Long operatorId,
                                           Long operatorOrgId) {
        try {
            if (agentRunRepository != null && StringUtils.hasText(runId)) {
                agentRunRepository.incrementToolCallCount(runId, orgId, 1);
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
                    .operatorOrgId(operatorOrgId)
                    .operatorType(operatorId == null ? "system" : "user")
                    .eventType("TOOL_INVOKE")
                    .resourceType("gateway_tool")
                    .resourceId(candidate.toolKey)
                    .resourceOrgId(orgId)
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

    /** 工具候选对象，承载构建 ToolCallback 所需的中间数据 */
    private static class ToolCandidate {

        private final Long toolId;
        private final String gatewayId;
        private final String toolName;
        private final String toolKey;
        private final String functionName;
        private final String description;
        private final String inputSchema;
        private final String riskLevel;

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
     * ToolCallback 包装器：暴露 toolKey，用于工具目录/治理。
     *
     * 说明：治理字段不应依赖 ToolDefinition.name（会因冲突治理而变化）。
     */
    private static final class GovernedToolCallback implements ToolCallback, ToolKeyAware {

        private final ToolCallback delegate;
        private final String toolKey;
        private final String toolSource;

        private GovernedToolCallback(ToolCallback delegate, String toolKey, String toolSource) {
            this.delegate = delegate;
            this.toolKey = toolKey;
            this.toolSource = toolSource;
        }

        /**
         * getToolDefinition。
         *
         * @return 返回结果
         */
        @Override
        public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
            return delegate.getToolDefinition();
        }

        /**
         * getToolMetadata。
         *
         * @return 返回结果
         */
        @Override
        public org.springframework.ai.tool.metadata.ToolMetadata getToolMetadata() {
            return delegate.getToolMetadata();
        }

        /**
         * call。
         *
         * @param toolInput 参数
         * @return 返回结果
         */
        @Override
        public String call(String toolInput) {
            return delegate.call(toolInput);
        }

        /**
         * call。
         *
         * @param toolInput 参数
         * @param toolContext 参数
         * @return 返回结果
         */
        @Override
        public String call(String toolInput, org.springframework.ai.chat.model.ToolContext toolContext) {
            return delegate.call(toolInput, toolContext);
        }

        /**
         * toolKey。
         *
         * @return 返回结果
         */
        @Override
        public String toolKey() {
            return toolKey;
        }

        /**
         * toolSource。
         *
         * @return 返回结果
         */
        @Override
        public String toolSource() {
            return toolSource;
        }
    }
}
