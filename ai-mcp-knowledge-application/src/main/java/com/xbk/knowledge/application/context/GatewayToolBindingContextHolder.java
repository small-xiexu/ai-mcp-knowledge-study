package com.xbk.knowledge.application.context;

import java.util.Collections;
import java.util.Set;

/**
 * Gateway 工具绑定上下文。
 *
 * 通过 ThreadLocal 传递当前会话与模型信息，便于按绑定关系过滤可见工具。
 *
 * @author sxie
 */
public final class GatewayToolBindingContextHolder {

    /**
     * ToolContext 中透传运行 ID 的键。
     */
    public static final String TOOL_CONTEXT_RUN_ID = "gateway.runId";

    /**
     * ToolContext 中透传操作人 ID 的键。
     */
    public static final String TOOL_CONTEXT_OPERATOR_ID = "gateway.operatorId";

    /**
     * ToolContext 中透传 `tool:invoke` 权限快照的键。
     */
    public static final String TOOL_CONTEXT_TOOL_INVOKE_PERMITTED = "gateway.toolInvokePermitted";

    private static final ThreadLocal<BindingContext> CONTEXT = new ThreadLocal<>();

    private GatewayToolBindingContextHolder() {
    }

    /**
     * 设置当前线程的绑定上下文。
     *
     * @param modelId 模型 ID
     * @param sessionId 会话 ID
     */
    public static void set(Long modelId, Long sessionId) {
        CONTEXT.set(new BindingContext(modelId, sessionId, null, null, null, null, null, null, null, null));
    }

    /**
     * 设置当前线程的绑定上下文（支持 AgentVersion allowlist）。
     * <p>
     * 说明：
     * - allowedToolKeys = null 表示“不启用 allowlist 过滤”（保持历史行为）。
     * - allowedToolKeys = 空集合 表示“明确无可用工具”。
     *
     * @param modelId 模型 ID
     * @param sessionId 会话 ID
     * @param agentVersionId 标识 ID
     * @param allowedToolKeys 允许调用的工具 Key 集合
     */
    public static void set(Long modelId, Long sessionId, Long agentVersionId, Set<String> allowedToolKeys) {
        set(modelId, sessionId, agentVersionId, null, allowedToolKeys);
    }

    /**
     * 设置当前线程的绑定上下文（支持 AgentVersion allowlist + runId）。
     * <p>
     * 说明：
     * - runId 通常与 traceId 对齐，用于工具调用审计与链路串联。
     *
     * @param modelId 模型 ID
     * @param sessionId 会话 ID
     * @param agentVersionId 标识 ID
     * @param runId 运行 ID
     * @param allowedToolKeys 允许调用的工具 Key 集合
     */
    public static void set(Long modelId,
                           Long sessionId,
                           Long agentVersionId,
                           String runId,
                           Set<String> allowedToolKeys) {
        set(modelId, sessionId, agentVersionId, runId, allowedToolKeys, null, null);
    }

    /**
     * 设置当前线程的绑定上下文（支持 AgentVersion allowlist + runId + 调用人权限快照）。
     *
     * @param modelId 模型 ID
     * @param sessionId 会话 ID
     * @param agentVersionId 标识 ID
     * @param runId 运行 ID
     * @param allowedToolKeys 允许调用的工具 Key 集合
     * @param operatorId 操作人 ID
     * @param toolInvokePermitted 是否具备 `tool:invoke` 权限
     */
    public static void set(Long modelId,
                           Long sessionId,
                           Long agentVersionId,
                           String runId,
                           Set<String> allowedToolKeys,
                           Long operatorId,
                           Boolean toolInvokePermitted) {
        Set<String> safeKeys = allowedToolKeys == null ? null : Collections.unmodifiableSet(allowedToolKeys);
        CONTEXT.set(new BindingContext(modelId, sessionId, agentVersionId, null, null, null, runId, safeKeys, operatorId, toolInvokePermitted));
    }

    /**
     * Workflow 场景绑定上下文（支持节点级 allowlist 与审批定位）。
     *
     * @param modelId 模型 ID
     * @param sessionId 会话 ID
     * @param workflowId 工作流 ID
     * @param workflowVersionId 工作流版本 ID
     * @param nodeKey 节点键
     * @param allowedToolKeys 允许调用的工具 Key 集合
     */
    public static void setWorkflow(Long modelId,
                                   Long sessionId,
                                   Long workflowId,
                                   Long workflowVersionId,
                                   String nodeKey,
                                   Set<String> allowedToolKeys) {
        setWorkflow(modelId, sessionId, workflowId, workflowVersionId, nodeKey, null, allowedToolKeys);
    }

    /**
     * Workflow 场景绑定上下文（支持节点级 allowlist、审批定位与 runId）。
     *
     * @param modelId 模型 ID
     * @param sessionId 会话 ID
     * @param workflowId 工作流 ID
     * @param workflowVersionId 工作流版本 ID
     * @param nodeKey 节点键
     * @param runId 运行 ID
     * @param allowedToolKeys 允许调用的工具 Key 集合
     */
    public static void setWorkflow(Long modelId,
                                   Long sessionId,
                                   Long workflowId,
                                   Long workflowVersionId,
                                   String nodeKey,
                                   String runId,
                                   Set<String> allowedToolKeys) {
        setWorkflow(modelId, sessionId, workflowId, workflowVersionId, nodeKey, runId, allowedToolKeys, null, null);
    }

    /**
     * Workflow 场景绑定上下文（支持节点级 allowlist、审批定位、runId 与调用人权限快照）。
     *
     * @param modelId 模型 ID
     * @param sessionId 会话 ID
     * @param workflowId 工作流 ID
     * @param workflowVersionId 工作流版本 ID
     * @param nodeKey 节点键
     * @param runId 运行 ID
     * @param allowedToolKeys 允许调用的工具 Key 集合
     * @param operatorId 操作人 ID
     * @param toolInvokePermitted 是否具备 `tool:invoke` 权限
     */
    public static void setWorkflow(Long modelId,
                                   Long sessionId,
                                   Long workflowId,
                                   Long workflowVersionId,
                                   String nodeKey,
                                   String runId,
                                   Set<String> allowedToolKeys,
                                   Long operatorId,
                                   Boolean toolInvokePermitted) {
        Set<String> safeKeys = allowedToolKeys == null ? null : Collections.unmodifiableSet(allowedToolKeys);
        CONTEXT.set(new BindingContext(modelId, sessionId, null, workflowId, workflowVersionId, nodeKey, runId, safeKeys, operatorId, toolInvokePermitted));
    }

    /**
     * 获取当前线程的绑定上下文。
     *
     * @return 当前线程绑定的上下文
     */
    public static BindingContext get() {
        return CONTEXT.get();
    }

    /**
     * 直接恢复指定上下文（通常用于跨线程透传后的还原）。
     *
     * @param context 需要恢复的上下文；为 `null` 时等价于 `clear()`
     */
    public static void set(BindingContext context) {
        if (context == null) {
            CONTEXT.remove();
            return;
        }
        CONTEXT.set(context);
    }

    /**
     * 清除当前线程的绑定上下文（必须在 finally 中调用，防止内存泄漏）。
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 绑定上下文，承载模型 ID 和会话 ID。
     */
    public static final class BindingContext {

        /**
         * 当前请求绑定的模型标识。
         */
        private final Long modelId;

        /**
         * 当前请求绑定的会话标识。
         */
        private final Long sessionId;

        /**
         * 当前会话使用的 Agent 版本标识。
         */
        private final Long agentVersionId;

        /**
         * 当前执行流程对应的 Workflow 标识。
         */
        private final Long workflowId;

        /**
         * 当前执行流程对应的 Workflow 版本标识。
         */
        private final Long workflowVersionId;

        /**
         * 当前执行到的 Workflow 节点标识。
         */
        private final String workflowNodeKey;

        /**
         * 当前执行链路的运行标识。
         */
        private final String runId;

        /**
         * 当前上下文允许调用的工具 Key 集合。
         */
        private final Set<String> allowedToolKeys;

        /**
         * 当前调用链路的操作人 ID（请求线程快照）。
         */
        private final Long operatorId;

        /**
         * 当前调用链路是否具备 `tool:invoke` 权限（请求线程快照）。
         */
        private final Boolean toolInvokePermitted;

        /**
         * 构建网关工具绑定上下文。
         *
         * @param modelId 模型 ID
         * @param sessionId 会话 ID
         * @param agentVersionId 智能体版本 ID
         * @param workflowId 工作流 ID
         * @param workflowVersionId 工作流版本 ID
         * @param workflowNodeKey 工作流节点 Key
         * @param runId 运行 ID
         * @param allowedToolKeys 允许调用的工具 Key 列表
         */
        private BindingContext(Long modelId,
                               Long sessionId,
                               Long agentVersionId,
                               Long workflowId,
                               Long workflowVersionId,
                               String workflowNodeKey,
                               String runId,
                               Set<String> allowedToolKeys,
                               Long operatorId,
                               Boolean toolInvokePermitted) {
            this.modelId = modelId;
            this.sessionId = sessionId;
            this.agentVersionId = agentVersionId;
            this.workflowId = workflowId;
            this.workflowVersionId = workflowVersionId;
            this.workflowNodeKey = workflowNodeKey;
            this.runId = runId;
            this.allowedToolKeys = allowedToolKeys;
            this.operatorId = operatorId;
            this.toolInvokePermitted = toolInvokePermitted;
        }

        /**
         * 获取当前绑定上下文中的模型 ID。
         *
         * @return 模型 ID
         */
        public Long getModelId() {
            return modelId;
        }

        /**
         * 获取当前绑定上下文中的会话 ID。
         *
         * @return 会话 ID
         */
        public Long getSessionId() {
            return sessionId;
        }

        /**
         * 获取当前绑定上下文中的 Agent 版本 ID。
         *
         * @return Agent 版本 ID
         */
        public Long getAgentVersionId() {
            return agentVersionId;
        }

        /**
         * 获取当前绑定上下文中的 Workflow ID。
         *
         * @return Workflow ID
         */
        public Long getWorkflowId() {
            return workflowId;
        }

        /**
         * 获取当前绑定上下文中的 Workflow 版本 ID。
         *
         * @return Workflow 版本 ID
         */
        public Long getWorkflowVersionId() {
            return workflowVersionId;
        }

        /**
         * 获取当前执行节点标识。
         *
         * @return Workflow 节点标识
         */
        public String getWorkflowNodeKey() {
            return workflowNodeKey;
        }

        /**
         * 获取本次执行链路的运行 ID。
         *
         * @return 运行 ID
         */
        public String getRunId() {
            return runId;
        }

        /**
         * 获取当前上下文允许调用的工具集合。
         *
         * @return 当前允许调用的工具 Key 集合
         */
        public Set<String> getAllowedToolKeys() {
            return allowedToolKeys;
        }

        /**
         * 获取当前操作人 ID。
         *
         * @return 操作人 ID
         */
        public Long getOperatorId() {
            return operatorId;
        }

        /**
         * 获取 `tool:invoke` 权限快照。
         *
         * @return 是否具备 `tool:invoke`；`null` 表示未写入快照
         */
        public Boolean getToolInvokePermitted() {
            return toolInvokePermitted;
        }
    }
}
