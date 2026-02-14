package com.xbk.knowledge.infrastructure.mcp;

import com.xbk.knowledge.domain.repository.AgentRunRepository;
import com.xbk.knowledge.domain.repository.AgentRunContextRepository;
import com.xbk.knowledge.domain.model.entity.approval.ApprovalRequest;
import com.xbk.knowledge.domain.model.entity.SysAuditEvent;
import com.xbk.knowledge.domain.repository.ApprovalRequestRepository;
import com.xbk.knowledge.domain.repository.ToolPolicyRepository;
import com.xbk.knowledge.infrastructure.audit.IdentityAuditLogService;
import com.xbk.knowledge.types.context.OrgContext;
import com.xbk.knowledge.types.context.OrgContextHolder;
import com.xbk.knowledge.types.exception.ApprovalRequiredException;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.tool.ToolNameUtils;
import com.xbk.knowledge.types.tool.ToolKeyAware;
import com.xbk.knowledge.types.tool.ToolInvokeBypassContextHolder;
import com.xbk.knowledge.types.trace.TraceIdUtils;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.LinkedHashMap;

import io.modelcontextprotocol.client.McpSyncClient;

/**
 * 动态 MCP 工具回调提供者
 * 根据运行时注册的 MCP Server 动态提供工具列表
 *
 * 职责：基础设施适配，用于提供可热更新的 ToolCallbackProvider
 * @author xiexu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicMcpToolCallbackProvider implements ToolCallbackProvider {

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final AtomicReference<List<McpClientDescriptor>> clients = new AtomicReference<>(Collections.emptyList());
    private final ConcurrentHashMap<Long, ToolCallback[]> cachedByOrg = new ConcurrentHashMap<>();

    private final AgentRunRepository agentRunRepository;
    private final AgentRunContextRepository agentRunContextRepository;
    private final IdentityAuditLogService auditLogService;
    private final ToolPolicyRepository toolPolicyRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private static final String PERMISSION_TOOL_INVOKE = "tool:invoke";

    /**
     * 更新 MCP 客户端列表
     *
     * 为什么：运行时连接变更后需要刷新工具回调
     * @param mcpClients MCP 客户端列表（带 orgId/serverName）
     */
    public void updateClients(List<McpClientDescriptor> mcpClients) {
        List<McpClientDescriptor> safeClients = mcpClients == null ? Collections.emptyList() : mcpClients;
        clients.set(safeClients);
        cachedByOrg.clear();
        int size = safeClients.size();
        log.info("MCP 工具回调更新完成，当前客户端数量: {}", size);
        /*
         * 目的：预热工具列表，避免首次查询命中空缓存
         */
        try {
            getToolCallbacks();
        } catch (Exception e) {
            log.warn("预热 MCP 工具回调失败", e);
        }
    }

    /**
     * 返回可用工具回调
     *
     * 为什么：按需构建并缓存工具回调数组
     * @return 工具回调列表
     */
    @Override
    public ToolCallback[] getToolCallbacks() {
        Long orgId = OrgContextHolder.currentOrgIdOrNull();
        if (orgId == null) {
            orgId = 1L;
        }

        ToolCallback[] base = cachedByOrg.get(orgId);
        if (base == null) {
            base = buildOrgCallbacks(orgId);
            cachedByOrg.put(orgId, base);
        }

        // allowlist 过滤：当上下文显式携带 allowedToolKeys 时生效
        Set<String> allowedToolKeys = resolveAllowedToolKeys();
        if (allowedToolKeys == null) {
            return base;
        }
        if (allowedToolKeys.isEmpty() || base.length == 0) {
            return new ToolCallback[0];
        }

        List<ToolCallback> filtered = new ArrayList<>();
        for (ToolCallback cb : base) {
            if (!(cb instanceof GovernedToolCallback governed)) {
                continue;
            }
            if (allowedToolKeys.contains(governed.toolKey)) {
                filtered.add(cb);
            }
        }
        return filtered.toArray(new ToolCallback[0]);
    }

    private ToolCallback[] buildOrgCallbacks(Long orgId) {
        List<McpClientDescriptor> snapshot = clients.get();
        int clientCount = snapshot == null ? 0 : snapshot.size();
        log.info("触发 MCP 工具回调获取，orgId: {}, 当前客户端数量: {}", orgId, clientCount);
        if (snapshot == null || snapshot.isEmpty()) {
            return new ToolCallback[0];
        }

        List<ToolCallback> merged = new ArrayList<>();
        for (McpClientDescriptor descriptor : snapshot) {
            if (descriptor == null || descriptor.client == null || descriptor.orgId == null) {
                continue;
            }
            if (!descriptor.orgId.equals(orgId)) {
                continue;
            }
            String serverName = descriptor.serverName == null ? "mcp" : descriptor.serverName;
            List<ToolCallback> callbacks = SyncMcpToolCallbackProvider.syncToolCallbacks(Collections.singletonList(descriptor.client));
            for (ToolCallback cb : callbacks) {
                if (cb == null || cb.getToolDefinition() == null || !StringUtils.hasText(cb.getToolDefinition().name())) {
                    continue;
                }
                String toolName = cb.getToolDefinition().name();
                String toolKey = "mcp:" + serverName + ":" + toolName;
                String functionName = ToolNameUtils.safeFunctionName("mcp", serverName, toolName);
                merged.add(new GovernedToolCallback(cb, toolKey, functionName));
            }
        }
        return merged.toArray(new ToolCallback[0]);
    }

    private Set<String> resolveAllowedToolKeys() {
        com.xbk.knowledge.application.context.GatewayToolBindingContextHolder.BindingContext ctx =
                com.xbk.knowledge.application.context.GatewayToolBindingContextHolder.get();
        return ctx == null ? null : ctx.getAllowedToolKeys();
    }

    /**
     * MCP 工具回调描述符（带 orgId/serverName）。
     *
     * 说明：用于在运行时强制 org 隔离，并为 toolKey 生成提供 serverName 维度。
     */
    public static final class McpClientDescriptor {
        private final Long orgId;
        private final String serverName;
        private final McpSyncClient client;

        public McpClientDescriptor(Long orgId, String serverName, McpSyncClient client) {
            this.orgId = orgId;
            this.serverName = serverName;
            this.client = client;
        }
    }

    /**
     * 带治理信息的 ToolCallback 包装器：
     * - function name 采用安全命名（避免跨 server 重名）
     * - call() 时写入 agent_run 工具计数 + sys_audit_event
     */
    private final class GovernedToolCallback implements ToolCallback, ToolKeyAware {

        private final ToolCallback delegate;
        private final String toolKey;
        private final ToolDefinition toolDefinition;
        private final String boundRunId;
        private final Long boundOrgId;
        private final Long boundOperatorId;
        private final Long boundOperatorOrgId;
        private final boolean bypassEnabled;
        private final boolean toolInvokePermitted;

        private GovernedToolCallback(ToolCallback delegate, String toolKey, String functionName) {
            this.delegate = delegate;
            this.toolKey = toolKey;
            ToolDefinition def = delegate.getToolDefinition();
            this.toolDefinition = DefaultToolDefinition.builder()
                    .name(functionName)
                    .description(def == null ? "" : def.description())
                    .inputSchema(def == null ? "{\"type\":\"object\",\"properties\":{}}" : def.inputSchema())
                    .build();

            /*
             * 重要：工具回调可能在异步/跨线程环境执行（例如流式/Reactive）。
             * 为保证治理门禁一致性（orgId、runId、权限、审计归属），在构建回调时捕获上下文快照，
             * call() 期间禁止再依赖 ThreadLocal（OrgContextHolder/StpUtil/MDC）获取关键字段。
             */
            this.boundRunId = TraceIdUtils.getOrCreateTraceId();
            Long orgId = OrgContextHolder.currentOrgIdOrNull();
            if (orgId == null) {
                orgId = 1L;
            }
            this.boundOrgId = orgId;
            OrgContext ctx = OrgContextHolder.get();
            this.boundOperatorId = ctx == null ? null : ctx.operatorUserId();
            this.boundOperatorOrgId = ctx == null ? null : ctx.operatorOrgId();
            this.bypassEnabled = ToolInvokeBypassContextHolder.isEnabled();
            boolean login = StpUtil.isLogin();
            this.toolInvokePermitted = !login || StpUtil.hasPermission(PERMISSION_TOOL_INVOKE);
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return toolDefinition;
        }

        @Override
        public org.springframework.ai.tool.metadata.ToolMetadata getToolMetadata() {
            return delegate.getToolMetadata();
        }

        @Override
        public String call(String arguments) {
            return call(arguments, null);
        }

        @Override
        public String call(String arguments, org.springframework.ai.chat.model.ToolContext toolContext) {
            String runId = boundRunId;
            Long orgId = boundOrgId;
            if (!bypassEnabled && !toolInvokePermitted) {
                recordToolDeniedAndAudit(runId, orgId, "PERMISSION_DENIED", boundOperatorId, boundOperatorOrgId);
                return "[PERMISSION_DENIED] 无权限调用工具（缺少权限: " + PERMISSION_TOOL_INVOKE + "），toolKey=" + toolKey;
            }
            String requesterType = boundOperatorId == null ? "system" : "user";
            maybeRequireApproval(runId, orgId, toolKey, arguments, boundOperatorId, requesterType, boundOperatorId, boundOperatorOrgId);
            long startAt = System.nanoTime();
            boolean success = false;
            String errorMessage = null;
            try {
                String result = toolContext == null ? delegate.call(arguments) : delegate.call(arguments, toolContext);
                success = true;
                return result;
            } catch (Exception e) {
                errorMessage = e.getMessage();
                throw e;
            } finally {
                long latencyMs = (System.nanoTime() - startAt) / 1_000_000;
                recordToolMetricsAndAudit(runId, orgId, success, latencyMs, errorMessage, boundOperatorId, boundOperatorOrgId);
            }
        }

        private void recordToolDeniedAndAudit(String runId, Long orgId, String reason, Long operatorId, Long operatorOrgId) {
            try {
                if (agentRunRepository != null && StringUtils.hasText(runId)) {
                    agentRunRepository.incrementToolDeniedCount(runId, orgId, 1);
                }
            } catch (Exception e) {
                log.warn("更新 agent_run 工具拒绝计数失败，runId: {}, toolKey: {}", runId, toolKey, e);
            }

            try {
                if (auditLogService == null || !StringUtils.hasText(toolKey)) {
                    return;
                }
                SysAuditEvent event = SysAuditEvent.builder()
                        .operatorId(operatorId)
                        .operatorOrgId(operatorOrgId)
                        .operatorType(operatorId == null ? "system" : "user")
                        .eventType("TOOL_INVOKE")
                        .resourceType("mcp_tool")
                        .resourceId(toolKey)
                        .resourceOrgId(orgId)
                        .action("DENIED")
                        .requestId(runId)
                        .result(0)
                        .errorMessage(reason)
                        .costMs(0L)
                        .build();
                auditLogService.record(event);
            } catch (Exception e) {
                log.warn("写入工具拒绝审计失败，runId: {}, toolKey: {}", runId, toolKey, e);
            }
        }

        private void recordToolMetricsAndAudit(String runId,
                                              Long orgId,
                                              boolean success,
                                              long latencyMs,
                                              String errorMessage,
                                              Long operatorId,
                                              Long operatorOrgId) {
            try {
                if (agentRunRepository != null && StringUtils.hasText(runId)) {
                    agentRunRepository.incrementToolCallCount(runId, orgId, 1);
                }
            } catch (Exception e) {
                log.warn("更新 agent_run 工具调用计数失败，runId: {}, toolKey: {}", runId, toolKey, e);
            }

            try {
                if (auditLogService == null || !StringUtils.hasText(toolKey)) {
                    return;
                }
                SysAuditEvent event = SysAuditEvent.builder()
                        .operatorId(operatorId)
                        .operatorOrgId(operatorOrgId)
                        .operatorType(operatorId == null ? "system" : "user")
                        .eventType("TOOL_INVOKE")
                        .resourceType("mcp_tool")
                        .resourceId(toolKey)
                        .resourceOrgId(orgId)
                        .action(success ? "SUCCESS" : "FAILED")
                        .requestId(runId)
                        .result(success ? 1 : 0)
                        .errorMessage(success ? null : errorMessage)
                        .costMs(latencyMs)
                        .build();
                auditLogService.record(event);
            } catch (Exception e) {
                log.warn("写入工具调用审计失败，runId: {}, toolKey: {}", runId, toolKey, e);
            }
        }

        @Override
        public String toolKey() {
            return toolKey;
        }

        @Override
        public String toolSource() {
            return "MCP";
        }

        /**
         * MCP 工具的高风险审批门禁（方式B）。
         *
         * 说明：
         * - MCP 工具缺少内置 risk_level 时，以 tool_policy 为准；未配置默认 MEDIUM
         * - riskLevel=HIGH 或 tool_policy.approval_required=1 时触发审批
         */
        private void maybeRequireApproval(String runId,
                                          Long orgId,
                                          String toolKey,
                                          String argumentsSnapshotJson,
                                          Long requesterId,
                                          String requesterType,
                                          Long operatorId,
                                          Long operatorOrgId) {
            if (!StringUtils.hasText(runId) || orgId == null || !StringUtils.hasText(toolKey) || approvalRequestRepository == null) {
                return;
            }
            String riskLevel = "MEDIUM";
            boolean approvalRequired = false;
            try {
                if (toolPolicyRepository != null) {
                    com.xbk.knowledge.domain.model.entity.tool.ToolPolicy policy =
                            toolPolicyRepository.findEnabled(orgId, toolKey).orElse(null);
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
                log.warn("查询 tool_policy 失败，orgId: {}, toolKey: {}", orgId, toolKey, e);
            }
            if (!approvalRequired && "HIGH".equalsIgnoreCase(riskLevel)) {
                approvalRequired = true;
            }
            if (!approvalRequired) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            try {
                if (approvalRequestRepository.findLatestApproved(orgId, runId, toolKey, now).isPresent()) {
                    return;
                }
            } catch (Exception e) {
                log.warn("查询 APPROVED 审批单失败，orgId: {}, runId: {}, toolKey: {}", orgId, runId, toolKey, e);
            }

            try {
                ApprovalRequest pending = approvalRequestRepository
                        .findLatestPending(orgId, runId, toolKey, now)
                        .orElse(null);
                if (pending != null && pending.getId() != null) {
                    throw new ApprovalRequiredException(pending.getId(), toolKey, riskLevel, "工具调用需要审批（已存在待审批单）");
                }
            } catch (ApprovalRequiredException e) {
                throw e;
            } catch (Exception e) {
                log.warn("查询 PENDING 审批单失败，orgId: {}, runId: {}, toolKey: {}", orgId, runId, toolKey, e);
            }

            com.xbk.knowledge.domain.model.entity.agent.AgentRun run = agentRunRepository
                    .findByRunId(orgId, runId)
                    .orElse(null);
            if (run == null || run.getAgentId() == null || run.getAgentVersionId() == null) {
                throw new BusinessException("工具审批门禁触发，但无法定位 agent_run 记录，runId=" + runId);
            }

            String snapshot = StringUtils.hasText(argumentsSnapshotJson) ? argumentsSnapshotJson : "{}";
            String digest = snapshot.length() <= 500 ? snapshot : snapshot.substring(0, 500);
            ApprovalRequest req = ApprovalRequest.builder()
                    .orgId(orgId)
                    .approvalType("TOOL_INVOKE")
                    .status("PENDING")
                    .runId(runId)
                    .agentId(run.getAgentId())
                    .agentVersionId(run.getAgentVersionId())
                    .requesterId(requesterId)
                    .requesterType(requesterType)
                    .toolKey(toolKey)
                    .riskLevel(riskLevel.toUpperCase(Locale.ROOT))
                    .argumentsSnapshotJson(snapshot)
                    .argumentsDigest(digest)
                    .expireAt(now.plusMinutes(30))
                    .build();
            approvalRequestRepository.insert(req);
            upsertPendingToolSnapshot(orgId, runId, toolKey, riskLevel, digest, req.getId(), now);
            throw new ApprovalRequiredException(req.getId(), toolKey, riskLevel, "工具调用需要审批（已生成审批单）");
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
    }
}
