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
     * 默认工具入参 Schema。
     */
    private static final String DEFAULT_TOOL_INPUT_SCHEMA = "{\"type\":\"object\",\"properties\":{}}";

    /**
     * 更新 MCP 客户端列表。
     * <p>
     * 大白话：
     * 1、把入参收敛成安全列表；
     * 2、原子替换 clients 快照并清空旧缓存；
     * 3、尝试预热一次，失败只告警，不阻断主流程。
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
     * 大白话：
     * 1、先读缓存，命中直接返回；
     * 2、未命中就构建并用 CAS 回填缓存（并发下允许少量重复构建）；
     * 3、最后按 allowlist 规则过滤：
     * `null` 不过滤、空集合返回空、非空集合仅保留命中的 toolKey。
     *
     * @return 可用工具回调数组。
     */
    @Override
    public ToolCallback[] getToolCallbacks() {
        // 1、读取全量工具缓存（未做 allowlist 过滤的基础集合）。
        ToolCallback[] base = cachedCallbacks.get();
        if (base == null) {
            // 缓存未命中时先构建一份基础集合。
            base = buildCallbacks();
            // CAS 只允许一个线程把结果写入缓存，其他并发线程即使也构建了，也以“已写入那份”为准。
            cachedCallbacks.compareAndSet(null, base);
            // 统一从缓存回读，保证后续逻辑使用同一份快照引用。
            base = cachedCallbacks.get();
        }

        // 2、按上下文 allowlist 做“视图级过滤”（不污染全量缓存）。
        // 约定：null 表示“调用方未提供 allowlist”，即不过滤，直接返回基础集合。
        Set<String> allowedToolKeys = resolveAllowedToolKeys();
        if (allowedToolKeys == null) {
            return base;
        }
        // 约定：空集合表示“显式禁止所有工具”；或基础集合本身为空时直接返回空数组。
        if (allowedToolKeys.isEmpty() || base.length == 0) {
            return new ToolCallback[0];
        }

        // 3、仅保留 toolKey 命中 allowlist 的治理包装回调。
        List<ToolCallback> filtered = new ArrayList<>();
        for (ToolCallback cb : base) {
            // 仅治理包装器才携带 toolKey，非治理回调不参与 allowlist 匹配。
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
     * <p>
     * 大白话：
     * 1、先把当前可用的 clients 拿一份快照；如果一个都没有，直接返回空数组；
     * 2、SyncMcpToolCallbackProvider 会驱动 McpSyncClient 调 client.listTools()，去问连接的对端 MCP Server：tools/list；
     * 3、拿到工具列表后，先把不合法/不可用的工具剔掉，再给每个工具生成统一的 toolKey 和 functionName；
     * 4、最后把这些有效工具都包成 GovernedToolCallback，返回给上层做缓存。
     *
     * @return 治理包装后的工具回调数组。
     */
    private ToolCallback[] buildCallbacks() {
        // 读取客户端快照，避免遍历过程中并发变更影响本次构建。
        List<McpClientDescriptor> snapshot = clients.get();
        int clientCount = 0;
        if (snapshot != null) {
            clientCount = snapshot.size();
        }
        log.info("触发 MCP 工具回调获取，当前客户端数量: {}", clientCount);
        // 没有可用 MCP 客户端时直接返回空结果。
        if (snapshot == null || snapshot.isEmpty()) {
            return new ToolCallback[0];
        }

        // 汇总所有客户端返回的工具，并在本地统一做治理包装。
        List<ToolCallback> merged = new ArrayList<>();
        // 遍历所有 MCP 客户端。
        for (McpClientDescriptor descriptor : snapshot) {
            // 跳过无效描述符，避免空指针干扰整体流程。
            if (descriptor == null || descriptor.client == null) {
                continue;
            }
            String serverName = descriptor.serverName == null ? "mcp" : descriptor.serverName;
            // 通过 listTools() 向对端 MCP Server 拉取该客户端可用工具。
            List<ToolCallback> callbacks = SyncMcpToolCallbackProvider.syncToolCallbacks(Collections.singletonList(descriptor.client));
            // 遍历当前 MCP Server 返回的工具列表
            for (ToolCallback cb : callbacks) {
                // 只保留定义完整且名称可用的工具。
                if (cb == null || !StringUtils.hasText(cb.getToolDefinition().name())) {
                    continue;
                }
                // 获取工具名称
                String toolName = cb.getToolDefinition().name();
                // 平台治理主键：用于 allowlist / 审批 / 审计，保持稳定可追溯。
                String toolKey = "mcp:" + serverName + ":" + toolName;
                // LLM 调用名：用于 ToolDefinition.name，需满足命名字符与长度约束。
                String functionName = ToolNameUtils.safeFunctionName("mcp", serverName, toolName);
                // 创建工具回调包装器
                GovernedToolCallback governedToolCallback = new GovernedToolCallback(cb, toolKey, functionName);
                merged.add(governedToolCallback);
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
         * @param client     MCP 客户端。
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
         * @param delegate     原始工具回调。
         * @param toolKey      平台治理主键（用于鉴权、审批、审计）。
         * @param functionName LLM 侧可调用的安全函数名（写入 ToolDefinition.name）。
         */
        private GovernedToolCallback(ToolCallback delegate, String toolKey, String functionName) {
            // 保存被包装的原始工具回调。
            this.delegate = delegate;
            // 保存平台治理主键，用于后续权限/审批/审计链路。
            this.toolKey = toolKey;
            // 读取原始工具定义，用于透传描述和入参 Schema。
            ToolDefinition def = delegate.getToolDefinition();
            // 默认描述为空字符串，避免下游出现 null。
            String description = "";
            // 默认入参 Schema 兜底为 object 空属性结构。
            String inputSchema = DEFAULT_TOOL_INPUT_SCHEMA;
            // 当原始工具定义存在时，优先使用原始描述与原始 Schema。
            if (def != null) {
                // 覆盖默认描述为原始工具描述。
                description = def.description();
                // 覆盖默认 Schema 为原始工具 Schema。
                inputSchema = def.inputSchema();
            }
            // 构建新的 ToolDefinition，并将函数名替换为安全命名后的名称。
            this.toolDefinition = DefaultToolDefinition.builder()
                    // 设置 LLM 可调用的函数名。
                    .name(functionName)
                    // 设置工具描述。
                    .description(description)
                    // 设置工具入参 Schema。
                    .inputSchema(inputSchema)
                    // 完成 ToolDefinition 构建。
                    .build();
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
         * <p>
         * 大白话：
         * 1、先拿到本次调用的追踪信息（runId + 操作人）；
         * 2、先过权限门禁（bypass 或 tool:invoke）；
         * 3、再过审批门禁（命中高风险会抛异常中断）；
         * 4、最后执行工具调用，并在 finally 统一记录指标和审计。
         *
         * @param arguments   工具调用参数。
         * @param toolContext 工具上下文。
         * @return 工具执行结果文本。
         */
        @Override
        public String call(String arguments, ToolContext toolContext) {
            BindingContext previousContext = GatewayToolBindingContextHolder.get();
            boolean contextBound = bindToolContext(toolContext);
            try {
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
            } finally {
                if (contextBound) {
                    GatewayToolBindingContextHolder.set(previousContext);
                }
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

        /**
         * 校验当前上下文是否允许调用工具。
         *
         * @return `true` 表示允许调用，`false` 表示无权限。
         */
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

        /**
         * 记录工具拒绝计数并写入审计日志。
         *
         * @param runId      运行 ID。
         * @param reason     拒绝原因。
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
         * @param runId        运行 ID。
         * @param success      是否调用成功。
         * @param latencyMs    调用耗时（毫秒）。
         * @param errorMessage 失败错误信息。
         * @param operatorId   操作人 ID。
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
         * 大白话：
         * 1、前置条件不满足直接跳过；
         * 2、MCP 动态工具默认 MEDIUM，只有 HIGH 才触发审批；
         * 3、已有 APPROVED 直接放行，已有 PENDING 直接复用并中断；
         * 4、都没有就创建审批单并抛异常中断本次调用；
         * 5、归属解析优先按 Workflow，上下文缺失再回退到 agent_run；
         * 6、会记录参数快照摘要（digest）和续跑所需上下文。
         *
         * @param runId                 运行 ID。
         * @param toolKey               工具键。
         * @param argumentsSnapshotJson 工具参数 JSON。
         * @param requesterId           标识 ID。
         * @param requesterType         请求者类型。
         * @param operatorId            操作人标识。
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

            BindingContext ctx = GatewayToolBindingContextHolder.get();
            workflowId = ctx == null ? null : ctx.getWorkflowId();
            workflowVersionId = ctx == null ? null : ctx.getWorkflowVersionId();
            workflowNodeKey = ctx == null ? null : ctx.getWorkflowNodeKey();
            if (workflowId == null || workflowVersionId == null || !StringUtils.hasText(workflowNodeKey)) {
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
            if (agentId != null && agentVersionId != null) {
                upsertPendingToolSnapshot(runId, toolKey, riskLevel, digest, req.getId(), now);
            }
            throw new ApprovalRequiredException(req.getId(), toolKey, riskLevel, "工具调用需要审批（已生成审批单）");
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
