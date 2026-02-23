package com.xbk.knowledge.application.context;

import java.util.Collections;
import java.util.Set;

/**
 * Gateway 工具绑定上下文
 * 通过 ThreadLocal 传递当前会话和模型信息
 * <p>
 * 为什么需要：GatewayToolCallbackProvider 在构建工具列表时需要知道当前请求的
 * 模型 ID 和会话 ID，以便按绑定关系过滤可见工具。由于 ToolCallbackProvider
 * 接口无法传递额外参数，因此通过 ThreadLocal 在调用链路中透传上下文
 *
 * @author sxie
 */
public final class GatewayToolBindingContextHolder {

    private static final ThreadLocal<BindingContext> CONTEXT = new ThreadLocal<>();

    private GatewayToolBindingContextHolder() {
    }

    /**
     * 设置当前线程的绑定上下文
     */
    public static void set(Long modelId, Long sessionId) {
        CONTEXT.set(new BindingContext(modelId, sessionId, null, null, null, null, null, null));
    }

    /**
     * 设置当前线程的绑定上下文（支持 AgentVersion allowlist）。
     * <p>
     * 说明：
     * - allowedToolKeys = null 表示“不启用 allowlist 过滤”（保持历史行为）。
     * - allowedToolKeys = 空集合 表示“明确无可用工具”。
     */
    public static void set(Long modelId, Long sessionId, Long agentVersionId, Set<String> allowedToolKeys) {
        set(modelId, sessionId, agentVersionId, null, allowedToolKeys);
    }

    /**
     * 设置当前线程的绑定上下文（支持 AgentVersion allowlist + runId）。
     * <p>
     * 说明：
     * - runId 通常与 traceId 对齐，用于工具调用审计与链路串联。
     */
    public static void set(Long modelId,
                           Long sessionId,
                           Long agentVersionId,
                           String runId,
                           Set<String> allowedToolKeys) {
        Set<String> safeKeys = allowedToolKeys == null ? null : Collections.unmodifiableSet(allowedToolKeys);
        CONTEXT.set(new BindingContext(modelId, sessionId, agentVersionId, null, null, null, runId, safeKeys));
    }

    /**
     * Workflow 场景绑定上下文（支持节点级 allowlist 与审批定位）。
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
     */
    public static void setWorkflow(Long modelId,
                                   Long sessionId,
                                   Long workflowId,
                                   Long workflowVersionId,
                                   String nodeKey,
                                   String runId,
                                   Set<String> allowedToolKeys) {
        Set<String> safeKeys = allowedToolKeys == null ? null : Collections.unmodifiableSet(allowedToolKeys);
        CONTEXT.set(new BindingContext(modelId, sessionId, null, workflowId, workflowVersionId, nodeKey, runId, safeKeys));
    }

    /**
     * 获取当前线程的绑定上下文
     */
    public static BindingContext get() {
        return CONTEXT.get();
    }

    /**
     * 清除当前线程的绑定上下文（必须在 finally 中调用，防止内存泄漏）
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 绑定上下文，承载模型 ID 和会话 ID
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

        private BindingContext(Long modelId,
                               Long sessionId,
                               Long agentVersionId,
                               Long workflowId,
                               Long workflowVersionId,
                               String workflowNodeKey,
                               String runId,
                               Set<String> allowedToolKeys) {
            this.modelId = modelId;
            this.sessionId = sessionId;
            this.agentVersionId = agentVersionId;
            this.workflowId = workflowId;
            this.workflowVersionId = workflowVersionId;
            this.workflowNodeKey = workflowNodeKey;
            this.runId = runId;
            this.allowedToolKeys = allowedToolKeys;
        }

        /**
         * 获取当前绑定上下文中的模型 ID。
         *
         * @return 返回模型 ID。
         */
        public Long getModelId() {
            return modelId;
        }

        /**
         * 获取当前绑定上下文中的会话 ID。
         *
         * @return 返回会话 ID。
         */
        public Long getSessionId() {
            return sessionId;
        }

        /**
         * 获取当前绑定上下文中的 Agent 版本 ID。
         *
         * @return 返回 Agent 版本 ID。
         */
        public Long getAgentVersionId() {
            return agentVersionId;
        }

        /**
         * 获取当前绑定上下文中的 Workflow ID。
         *
         * @return 返回 Workflow ID。
         */
        public Long getWorkflowId() {
            return workflowId;
        }

        /**
         * 获取当前绑定上下文中的 Workflow 版本 ID。
         *
         * @return 返回 Workflow 版本 ID。
         */
        public Long getWorkflowVersionId() {
            return workflowVersionId;
        }

        /**
         * 获取当前执行节点标识。
         *
         * @return 返回 Workflow 节点标识。
         */
        public String getWorkflowNodeKey() {
            return workflowNodeKey;
        }

        /**
         * 获取本次执行链路的运行 ID。
         *
         * @return 返回运行 ID。
         */
        public String getRunId() {
            return runId;
        }

        /**
         * 获取当前上下文允许调用的工具集合。
         *
         * @return 返回 Set<String> 数据。
         */
        public Set<String> getAllowedToolKeys() {
            return allowedToolKeys;
        }
    }
}
