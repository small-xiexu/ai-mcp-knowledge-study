package com.xbk.knowledge.infrastructure.mcp;

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
import com.xbk.knowledge.types.json.JsonMapUtils;
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
import java.util.Optional;

import io.modelcontextprotocol.client.McpSyncClient;

/**
 * 动态 MCP 工具回调提供者
 * 根据运行时注册的 MCP Server 动态提供工具列表
 * <p>
 * 职责：基础设施适配，用于提供可热更新的 ToolCallbackProvider
 *
 * @author sxie
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicMcpToolCallbackProvider implements ToolCallbackProvider {

    /**
     * JSON 序列化器。
     */
    private final ObjectMapper objectMapper;

    /**
     * MCP 客户端快照引用。
     */
    private final AtomicReference<List<McpClientDescriptor>> clients = new AtomicReference<>(Collections.emptyList());

    /**
     * 工具回调缓存引用。
     * 缓存当前 MCP 客户端快照生成的回调数组。
     */
    private final AtomicReference<ToolCallback[]> cachedCallbacks = new AtomicReference<>(null);

    /**
     * Agent 运行记录仓储。
     */
    private final AgentRunRepository agentRunRepository;

    /**
     * Agent 运行上下文仓储。
     */
    private final AgentRunContextRepository agentRunContextRepository;

    /**
     * Workflow 节点运行仓储。
     */
    private final WorkflowNodeRunRepository workflowNodeRunRepository;

    /**
     * 身份审计日志服务。
     */
    private final IdentityAuditLogService auditLogService;

    /**
     * 审批请求仓储。
     */
    private final ApprovalRequestRepository approvalRequestRepository;

    /**
     * 工具调用权限码。
     */
    private static final String PERMISSION_TOOL_INVOKE = "tool:invoke";

    /**
     * 更新 MCP 客户端列表。
     * <p>
     * 运行时连接变更后刷新工具回调缓存。
     * 
     * @param mcpClients MCP 客户端描述符列表（包含 serverName）。
     */
    public void updateClients(List<McpClientDescriptor> mcpClients) {
        List<McpClientDescriptor> safeClients = Optional.ofNullable(mcpClients).orElse(Collections.emptyList());
        clients.set(safeClients);
        cachedCallbacks.set(null);
        int size = safeClients.size();
        log.info("MCP 工具回调更新完成，当前客户端数量: {}", size);
        // 预热工具列表，避免首次查询命中空缓存
        try {
            getToolCallbacks();
        } catch (Exception e) {
            log.warn("预热 MCP 工具回调失败", e);
        }
    }

    /**
     * 获取可用工具回调数组。
     * <p>
     * 按需构建并缓存工具回调数组。
     * 
     * @return 可用工具回调数组。
     */
    @Override
    public ToolCallback[] getToolCallbacks() {
        ToolCallback[] base = cachedCallbacks.get();
        if (base == null) {
            base = buildCallbacks();
            cachedCallbacks.compareAndSet(null, base);
            base = cachedCallbacks.get();
        }

        // allowlist 过滤当上下文显式携带 allowedToolKeys 时生效
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

    /**
     * 基于当前 MCP 客户端快照构建工具回调数组。
     *
     * @return 治理包装后的工具回调数组。
     */
    private ToolCallback[] buildCallbacks() {
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
                if (cb == null || !StringUtils.hasText(cb.getToolDefinition().name())) {
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

    /**
     * 解析当前上下文中的工具 allowlist。
     *
     * @return 允许调用的工具键集合；`null` 表示不过滤。
     */
    private Set<String> resolveAllowedToolKeys() {
        BindingContext ctx = GatewayToolBindingContextHolder.get();
        return ctx == null ? null : ctx.getAllowedToolKeys();
    }

    /**
     * MCP 工具回调描述符（serverName + client）。
     */
    public static final class McpClientDescriptor {
        /**
         * MCP 服务名称。
         */
        private final String serverName;

        /**
         * MCP 同步客户端。
         */
        private final McpSyncClient client;

        /**
         * 构造 MCP 客户端描述符。
         * 
         * @param serverName MCP 服务端名称。
         * @param client MCP 客户端。
         */
        public McpClientDescriptor(String serverName, McpSyncClient client) {
            this.serverName = serverName;
            this.client = client;
        }
    }

    /**
     * 带治理信息的 ToolCallback 包装器
     * - function name 采用安全命名（避免跨 server 重名）
     * - call() 时写入 agent_run 工具计数 + sys_audit_event
     */
    private final class GovernedToolCallback implements ToolCallback, ToolKeyAware {

        /**
         * 被包装的原始工具回调。
         */
        private final ToolCallback delegate;

        /**
         * 工具唯一标识。
         */
        private final String toolKey;

        /**
         * 安全命名后的工具定义。
         */
        private final ToolDefinition toolDefinition;

        /**
         * 构造带治理能力的工具回调包装器。
         *
         * @param delegate 原始工具回调。
         * @param toolKey 工具唯一键。
         * @param functionName 安全命名后的函数名。
         */
        private GovernedToolCallback(ToolCallback delegate, String toolKey, String functionName) {
            this.delegate = delegate;
            this.toolKey = toolKey;
            ToolDefinition def = delegate.getToolDefinition();
            this.toolDefinition = DefaultToolDefinition.builder().name(functionName).description(def == null ? "" : def.description()).inputSchema(def == null ? "{\"type\":\"object\",\"properties\":{}}" : def.inputSchema()).build();
        }

        /**
         * 获取工具定义。
         * 
         * @return 工具定义。
         */
        @Override
        public ToolDefinition getToolDefinition() {
            return toolDefinition;
        }

        /**
         * 获取工具元数据。
         * 
         * @return 工具元数据。
         */
        @Override
        public ToolMetadata getToolMetadata() {
            return delegate.getToolMetadata();
        }

        /**
         * 调用 MCP 工具（无上下文）。
         * 
         * @param arguments 工具调用参数。
         * @return 工具执行结果文本。
         */
        @Override
        public String call(String arguments) {
            return call(arguments, null);
        }

        /**
         * 调用 MCP 工具（带上下文）。
         * 
         * @param arguments 工具调用参数。
         * @param toolContext 工具上下文。
         * @return 工具执行结果文本。
         */
        @Override
        public String call(String arguments, ToolContext toolContext) {
            String runId = resolveRunId();
            Long operatorId = resolveOperatorId();
            boolean bypassEnabled = ToolInvokeBypassContextHolder.isEnabled();
            boolean toolInvokePermitted = isToolInvokePermitted();
            if (!bypassEnabled && !toolInvokePermitted) {
                recordToolDeniedAndAudit(runId, "PERMISSION_DENIED", operatorId);
                return "[PERMISSION_DENIED] 无权限调用工具（缺少权限: " + PERMISSION_TOOL_INVOKE + "），toolKey=" + toolKey;
            }
            String requesterType = operatorId == null ? "system" : "user";
            maybeRequireApproval(runId, toolKey, arguments, operatorId, requesterType, operatorId);
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
                recordToolMetricsAndAudit(runId, success, latencyMs, errorMessage, operatorId);
            }
        }

        /**
         * 解析当前工具调用对应的运行 ID。
         *
         * @return 运行 ID。
         */
        private String resolveRunId() {
            BindingContext context = GatewayToolBindingContextHolder.get();
            if (context != null && StringUtils.hasText(context.getRunId())) {
                return context.getRunId();
            }
            return TraceIdUtils.getOrCreateTraceId();
        }

        /**
         * 解析当前操作人 ID。
         *
         * @return 当前登录用户 ID；未登录或解析失败时返回 `null`。
         */
        private Long resolveOperatorId() {
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

        /**
         * 校验当前上下文是否允许调用工具。
         *
         * @return `true` 表示允许调用，`false` 表示无权限。
         */
        private boolean isToolInvokePermitted() {
            try {
                return !StpUtil.isLogin() || StpUtil.hasPermission(PERMISSION_TOOL_INVOKE);
            } catch (Exception ignore) {
                return false;
            }
        }

        /**
         * 记录工具拒绝计数并写入审计日志。
         *
         * @param runId 运行 ID。
         * @param reason 拒绝原因。
         * @param operatorId 操作人 ID。
         */
        private void recordToolDeniedAndAudit(String runId, String reason, Long operatorId) {
            try {
                if (agentRunRepository != null && StringUtils.hasText(runId)) {
                    agentRunRepository.incrementToolDeniedCount(runId, 1);
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
                SysAuditEvent event = SysAuditEvent.builder().operatorId(operatorId).operatorType(operatorId == null ? "system" : "user").eventType("TOOL_INVOKE").resourceType("mcp_tool").resourceId(toolKey).action("DENIED").requestId(runId).result(0).errorMessage(reason).costMs(0L).build();
                auditLogService.record(event);
            } catch (Exception e) {
                log.warn("写入工具拒绝审计失败，runId: {}, toolKey: {}", runId, toolKey, e);
            }
        }

        /**
         * 记录工具调用指标并写入审计日志。
         *
         * @param runId 运行 ID。
         * @param success 是否调用成功。
         * @param latencyMs 调用耗时（毫秒）。
         * @param errorMessage 失败错误信息。
         * @param operatorId 操作人 ID。
         */
        private void recordToolMetricsAndAudit(String runId, boolean success, long latencyMs, String errorMessage, Long operatorId) {
            try {
                if (agentRunRepository != null && StringUtils.hasText(runId)) {
                    agentRunRepository.incrementToolCallCount(runId, 1);
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
                SysAuditEvent event = SysAuditEvent.builder().operatorId(operatorId).operatorType(operatorId == null ? "system" : "user").eventType("TOOL_INVOKE").resourceType("mcp_tool").resourceId(toolKey).action(success ? "SUCCESS" : "FAILED").requestId(runId).result(success ? 1 : 0).errorMessage(success ? null : errorMessage).costMs(latencyMs).build();
                auditLogService.record(event);
            } catch (Exception e) {
                log.warn("写入工具调用审计失败，runId: {}, toolKey: {}", runId, toolKey, e);
            }
        }

        /**
         * 获取当前工具标识。
         * 
         * @return 工具唯一标识。
         */
        @Override
        public String toolKey() {
            return toolKey;
        }

        /**
         * 获取工具来源类型。
         * 
         * @return 固定来源标识。
         */
        @Override
        public String toolSource() {
            return "MCP";
        }

        /**
         * MCP 工具的高风险审批门禁（方式B）。
         * <p>
         * 说明：
         * - MCP 工具按默认 MEDIUM 风险处理
         * - riskLevel=HIGH 时触发审批
         * 
         * @param runId 运行 ID。
         * @param toolKey 工具键。
         * @param argumentsSnapshotJson 工具参数 JSON。
         * @param requesterId 标识 ID。
         * @param requesterType 请求者类型。
         * @param operatorId 操作人标识。
         */
        private void maybeRequireApproval(String runId, String toolKey, String argumentsSnapshotJson, Long requesterId, String requesterType, Long operatorId) {
            if (!StringUtils.hasText(runId) || !StringUtils.hasText(toolKey) || approvalRequestRepository == null) {
                return;
            }
            String riskLevel = "MEDIUM";
            if (!"HIGH".equalsIgnoreCase(riskLevel)) {
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
                ApprovalRequest pending = approvalRequestRepository.findLatestPending(runId, toolKey, now).orElse(null);
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
                // fallback按 agent_run 归属
                AgentRun run = agentRunRepository.findByRunId(runId).orElse(null);
                if (run != null && run.getAgentId() != null && run.getAgentVersionId() != null) {
                    agentId = run.getAgentId();
                    agentVersionId = run.getAgentVersionId();
                } else {
                    throw new BusinessException("工具审批门禁触发，但无法定位运行归属（agent/workflow），runId=" + runId);
                }
            }

            String snapshot = StringUtils.hasText(argumentsSnapshotJson) ? argumentsSnapshotJson : "{}";
            String digest = snapshot.length() <= 500 ? snapshot : snapshot.substring(0, 500);
            ApprovalRequest req = ApprovalRequest.builder().approvalType("TOOL_INVOKE").status("PENDING").runId(runId).agentId(agentId).agentVersionId(agentVersionId).workflowId(workflowId).workflowVersionId(workflowVersionId).nodeKey(workflowNodeKey).requesterId(requesterId).requesterType(requesterType).toolKey(toolKey).riskLevel(riskLevel.toUpperCase(Locale.ROOT)).argumentsSnapshotJson(snapshot).argumentsDigest(digest).expireAt(now.plusMinutes(30)).build();
            approvalRequestRepository.insert(req);
            // agent_run_context 续跑快照由 Agent 侧维护；Workflow 场景由 WorkflowRuntime 侧写入 workflow_run_context
            if (agentId != null && agentVersionId != null) {
                upsertPendingToolSnapshot(runId, toolKey, riskLevel, digest, req.getId(), now);
            }
            throw new ApprovalRequiredException(req.getId(), toolKey, riskLevel, "工具调用需要审批（已生成审批单）");
        }

        /**
         * 更新待审批工具快照。
         * 
         * @param runId 运行ID。
         * @param toolKey 工具标识。
         * @param riskLevel 风险级别。
         * @param argsDigest 参数摘要。
         * @param approvalRequestId 审批申请ID。
         * @param now 当前时间。
         */
        private void upsertPendingToolSnapshot(String runId, String toolKey, String riskLevel, String argsDigest, Long approvalRequestId, LocalDateTime now) {
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
                        }
                    }
                });
                map.put("pendingToolKey", toolKey);
                map.put("pendingRiskLevel", riskLevel);
                map.put("pendingArgumentsDigest", argsDigest);
                map.put("approvalRequestId", approvalRequestId);
                map.put("pendingAt", now == null ? null : now.toString());

                String json = objectMapper.writeValueAsString(map);
                AgentRunContext ctx = AgentRunContext.builder().runId(runId).status("SAVED").snapshotJson(json).build();
                agentRunContextRepository.upsert(ctx);
            } catch (Exception e) {
                log.warn("更新 agent_run_context 待审批快照失败，runId: {}, toolKey: {}", runId, toolKey, e);
            }
        }
    }
}
