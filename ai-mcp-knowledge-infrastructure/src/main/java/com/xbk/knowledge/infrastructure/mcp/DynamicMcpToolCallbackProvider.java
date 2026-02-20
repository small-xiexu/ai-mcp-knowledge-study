package com.xbk.knowledge.infrastructure.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.context.GatewayToolBindingContextHolder;
import com.xbk.knowledge.application.context.GatewayToolBindingContextHolder.BindingContext;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentRunRepository;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentRunContextRepository;
import com.xbk.knowledge.domain.agent.model.entity.AgentRun;
import com.xbk.knowledge.domain.agent.model.entity.AgentRunContext;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowNodeRunRepository;
import com.xbk.knowledge.domain.approval.model.entity.ApprovalRequest;
import com.xbk.knowledge.domain.audit.model.entity.SysAuditEvent;
import com.xbk.knowledge.domain.approval.adapter.repository.ApprovalRequestRepository;
import com.xbk.knowledge.infrastructure.audit.IdentityAuditLogService;
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
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
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
 * @author sxie
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicMcpToolCallbackProvider implements ToolCallbackProvider {

    private final ObjectMapper objectMapper;
    private final AtomicReference<List<McpClientDescriptor>> clients = new AtomicReference<>(Collections.emptyList());
    private final ConcurrentHashMap<Long, ToolCallback[]> cachedByOrg = new ConcurrentHashMap<>();

    private final AgentRunRepository agentRunRepository;
    private final AgentRunContextRepository agentRunContextRepository;
    private final WorkflowNodeRunRepository workflowNodeRunRepository;
    private final IdentityAuditLogService auditLogService;
    private final ApprovalRequestRepository approvalRequestRepository;
    private static final String PERMISSION_TOOL_INVOKE = "tool:invoke";

    /**
     * 更新 MCP 客户端列表
     *
     * 为什么：运行时连接变更后需要刷新工具回调
     * @param mcpClients MCP 客户端列表（带 scopeId/serverName）
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
        ToolCallback[] base = cachedByOrg.get(1L);
        if (base == null) {
            base = buildOrgCallbacks();
            cachedByOrg.put(1L, base);
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

    private ToolCallback[] buildOrgCallbacks() {
        List<McpClientDescriptor> snapshot = clients.get();
        int clientCount = snapshot == null ? 0 : snapshot.size();
        log.info("触发 MCP 工具回调获取，当前客户端数量: {}", clientCount);
        if (snapshot == null || snapshot.isEmpty()) {
            return new ToolCallback[0];
        }

        List<ToolCallback> merged = new ArrayList<>();
        for (McpClientDescriptor descriptor : snapshot) {
            if (descriptor == null || descriptor.client == null) {
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
        BindingContext ctx = GatewayToolBindingContextHolder.get();
        return ctx == null ? null : ctx.getAllowedToolKeys();
    }

    /** MCP 工具回调描述符（serverName + client）。 */
    public static final class McpClientDescriptor {
        private final String serverName;
        private final McpSyncClient client;

        /**
         * McpClientDescriptor。
         *
         * @param serverName 参数
         * @param client 参数
         */
        public McpClientDescriptor(String serverName, McpSyncClient client) {
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
        private final Long boundScopeId;
        private final Long boundOperatorId;
        private final Long boundOperatorScopeId;
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
             * 为保证治理门禁一致性（scopeId、runId、权限、审计归属），在构建回调时捕获上下文快照，
             * call() 期间禁止再依赖线程上下文（如 StpUtil/MDC）获取关键字段。
 */
            this.boundRunId = TraceIdUtils.getOrCreateTraceId();
            this.boundScopeId = 1L;
            this.boundOperatorId = null;
            this.boundOperatorScopeId = 1L;
            this.bypassEnabled = ToolInvokeBypassContextHolder.isEnabled();
            boolean login = StpUtil.isLogin();
            this.toolInvokePermitted = !login || StpUtil.hasPermission(PERMISSION_TOOL_INVOKE);
        }

        /**
         * getToolDefinition。
         *
         * @return 返回结果
         */
        @Override
        public ToolDefinition getToolDefinition() {
            return toolDefinition;
        }

        /**
         * getToolMetadata。
         *
         * @return 返回结果
         */
        @Override
        public ToolMetadata getToolMetadata() {
            return delegate.getToolMetadata();
        }

        /**
         * call。
         *
         * @param arguments 参数
         * @return 返回结果
         */
        @Override
        public String call(String arguments) {
            return call(arguments, null);
        }

        /**
         * call。
         *
         * @param arguments 参数
         * @param toolContext 参数
         * @return 返回结果
         */
        @Override
        public String call(String arguments, ToolContext toolContext) {
            String runId = boundRunId;
            if (!bypassEnabled && !toolInvokePermitted) {
                recordToolDeniedAndAudit(runId,  "PERMISSION_DENIED", boundOperatorId, boundOperatorScopeId);
                return "[PERMISSION_DENIED] 无权限调用工具（缺少权限: " + PERMISSION_TOOL_INVOKE + "），toolKey=" + toolKey;
            }
            String requesterType = boundOperatorId == null ? "system" : "user";
            maybeRequireApproval(runId,  toolKey, arguments, boundOperatorId, requesterType, boundOperatorId, boundOperatorScopeId);
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
                recordToolMetricsAndAudit(runId,  success, latencyMs, errorMessage, boundOperatorId, boundOperatorScopeId);
            }
        }

        private void recordToolDeniedAndAudit(String runId,  String reason, Long operatorId, Long operatorScopeId) {
            try {
                if (agentRunRepository != null && StringUtils.hasText(runId)) {
                    agentRunRepository.incrementToolDeniedCount(runId,  1);
                }
                BindingContext ctx = GatewayToolBindingContextHolder.get();
                String nodeKey = ctx == null ? null : ctx.getWorkflowNodeKey();
                if (workflowNodeRunRepository != null && StringUtils.hasText(runId) && StringUtils.hasText(nodeKey)) {
                    workflowNodeRunRepository.incrementToolDeniedCount(runId, nodeKey, 1);
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
                        .operatorScopeId(operatorScopeId)
                        .operatorType(operatorId == null ? "system" : "user")
                        .eventType("TOOL_INVOKE")
                        .resourceType("mcp_tool")
                        .resourceId(toolKey)
                        .resourceScopeId(boundScopeId)
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
                                              boolean success,
                                              long latencyMs,
                                              String errorMessage,
                                              Long operatorId,
                                              Long operatorScopeId) {
            try {
                if (agentRunRepository != null && StringUtils.hasText(runId)) {
                    agentRunRepository.incrementToolCallCount(runId,  1);
                }
                BindingContext ctx = GatewayToolBindingContextHolder.get();
                String nodeKey = ctx == null ? null : ctx.getWorkflowNodeKey();
                if (workflowNodeRunRepository != null && StringUtils.hasText(runId) && StringUtils.hasText(nodeKey)) {
                    workflowNodeRunRepository.incrementToolCallCount(runId, nodeKey, 1);
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
                        .operatorScopeId(operatorScopeId)
                        .operatorType(operatorId == null ? "system" : "user")
                        .eventType("TOOL_INVOKE")
                        .resourceType("mcp_tool")
                        .resourceId(toolKey)
                        .resourceScopeId(boundScopeId)
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
            return "MCP";
        }

        /**
         * MCP 工具的高风险审批门禁（方式B）。
         *
         * 说明：
         * - MCP 工具按默认 MEDIUM 风险处理
         * - riskLevel=HIGH 时触发审批
         */
        private void maybeRequireApproval(String runId, 
                                          String toolKey,
                                          String argumentsSnapshotJson,
                                          Long requesterId,
                                          String requesterType,
                                          Long operatorId,
                                          Long operatorScopeId) {
            if (!StringUtils.hasText(runId) || !StringUtils.hasText(toolKey) || approvalRequestRepository == null) {
                return;
            }
            String riskLevel = "MEDIUM";
            boolean approvalRequired = false;
            if (!approvalRequired && "HIGH".equalsIgnoreCase(riskLevel)) {
                approvalRequired = true;
            }
            if (!approvalRequired) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            try {
                if (approvalRequestRepository.findLatestApproved(runId, toolKey, now).isPresent()) {
                    return;
                }
            } catch (Exception e) {
                log.warn("查询 APPROVED 审批单失败，runId: {}, toolKey: {}", runId, toolKey, e);
            }

            try {
                ApprovalRequest pending = approvalRequestRepository
                        .findLatestPending(runId, toolKey, now)
                        .orElse(null);
                if (pending != null && pending.getId() != null) {
                    throw new ApprovalRequiredException(pending.getId(), toolKey, riskLevel, "工具调用需要审批（已存在待审批单）");
                }
            } catch (ApprovalRequiredException e) {
                throw e;
            } catch (Exception e) {
                log.warn("查询 PENDING 审批单失败，runId: {}, toolKey: {}", runId, toolKey, e);
            }

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
                // fallback：按 agent_run 归属
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

            String snapshot = StringUtils.hasText(argumentsSnapshotJson) ? argumentsSnapshotJson : "{}";
            String digest = snapshot.length() <= 500 ? snapshot : snapshot.substring(0, 500);
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
                    .toolKey(toolKey)
                    .riskLevel(riskLevel.toUpperCase(Locale.ROOT))
                    .argumentsSnapshotJson(snapshot)
                    .argumentsDigest(digest)
                    .expireAt(now.plusMinutes(30))
                    .build();
            approvalRequestRepository.insert(req);
            // agent_run_context 续跑快照由 Agent 侧维护；Workflow 场景由 WorkflowRuntime 侧写入 workflow_run_context
            if (agentId != null && agentVersionId != null) {
                upsertPendingToolSnapshot(runId, toolKey, riskLevel, digest, req.getId(), now);
            }
            throw new ApprovalRequiredException(req.getId(), toolKey, riskLevel, "工具调用需要审批（已生成审批单）");
        }

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
                            Map<String, Object> existed = objectMapper.readValue(
                                    ctx.getSnapshotJson(),
                                    new TypeReference<Map<String, Object>>() {}
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
    }
}
