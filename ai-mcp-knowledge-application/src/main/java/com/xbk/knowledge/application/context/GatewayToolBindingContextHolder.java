package com.xbk.knowledge.application.context;

import java.util.Collections;
import java.util.Set;

/**
 * Gateway 工具绑定上下文
 * 通过 ThreadLocal 传递当前会话和模型信息
 *
 * 为什么需要：GatewayToolCallbackProvider 在构建工具列表时需要知道当前请求的
 * 模型 ID 和会话 ID，以便按绑定关系过滤可见工具。由于 ToolCallbackProvider
 * 接口无法传递额外参数，因此通过 ThreadLocal 在调用链路中透传上下文
 *
 * @author xiexu
 */
public final class GatewayToolBindingContextHolder {

    private static final ThreadLocal<BindingContext> CONTEXT = new ThreadLocal<>();

    private GatewayToolBindingContextHolder() {
    }

    /** 设置当前线程的绑定上下文 */
    public static void set(Long modelId, Long sessionId) {
        CONTEXT.set(new BindingContext(modelId, sessionId, null, null, null, null, null));
    }

    /**
     * 设置当前线程的绑定上下文（支持 AgentVersion allowlist）。
     *
     * 说明：
     * - allowedToolKeys = null 表示“不启用 allowlist 过滤”（保持历史行为）。
     * - allowedToolKeys = 空集合 表示“明确无可用工具”。
     */
    public static void set(Long modelId, Long sessionId, Long agentVersionId, Set<String> allowedToolKeys) {
        Set<String> safeKeys = allowedToolKeys == null ? null : Collections.unmodifiableSet(allowedToolKeys);
        CONTEXT.set(new BindingContext(modelId, sessionId, agentVersionId, null, null, null, safeKeys));
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
        Set<String> safeKeys = allowedToolKeys == null ? null : Collections.unmodifiableSet(allowedToolKeys);
        CONTEXT.set(new BindingContext(modelId, sessionId, null, workflowId, workflowVersionId, nodeKey, safeKeys));
    }

    /** 获取当前线程的绑定上下文 */
    public static BindingContext get() {
        return CONTEXT.get();
    }

    /** 清除当前线程的绑定上下文（必须在 finally 中调用，防止内存泄漏） */
    public static void clear() {
        CONTEXT.remove();
    }

    /** 绑定上下文，承载模型 ID 和会话 ID */
    public static final class BindingContext {

        private final Long modelId;
        private final Long sessionId;
        private final Long agentVersionId;
        private final Long workflowId;
        private final Long workflowVersionId;
        private final String workflowNodeKey;
        private final Set<String> allowedToolKeys;

        private BindingContext(Long modelId,
                               Long sessionId,
                               Long agentVersionId,
                               Long workflowId,
                               Long workflowVersionId,
                               String workflowNodeKey,
                               Set<String> allowedToolKeys) {
            this.modelId = modelId;
            this.sessionId = sessionId;
            this.agentVersionId = agentVersionId;
            this.workflowId = workflowId;
            this.workflowVersionId = workflowVersionId;
            this.workflowNodeKey = workflowNodeKey;
            this.allowedToolKeys = allowedToolKeys;
        }

        /**
         * getModelId。
         *
         * @return 返回结果
         */
        public Long getModelId() {
            return modelId;
        }

        /**
         * getSessionId。
         *
         * @return 返回结果
         */
        public Long getSessionId() {
            return sessionId;
        }

        /**
         * getAgentVersionId。
         *
         * @return 返回结果
         */
        public Long getAgentVersionId() {
            return agentVersionId;
        }

        /**
         * getWorkflowId。
         *
         * @return 返回结果
         */
        public Long getWorkflowId() {
            return workflowId;
        }

        /**
         * getWorkflowVersionId。
         *
         * @return 返回结果
         */
        public Long getWorkflowVersionId() {
            return workflowVersionId;
        }

        /**
         * getWorkflowNodeKey。
         *
         * @return 返回结果
         */
        public String getWorkflowNodeKey() {
            return workflowNodeKey;
        }

        /**
         * getAllowedToolKeys。
         *
         * @return 返回结果
         */
        public Set<String> getAllowedToolKeys() {
            return allowedToolKeys;
        }
    }
}
