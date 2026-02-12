package com.xbk.knowledge.application.context;

/**
 * Gateway 工具绑定上下文
 * 通过 ThreadLocal 传递当前会话和模型信息
 *
 * @author xiexu
 */
public final class GatewayToolBindingContextHolder {

    private static final ThreadLocal<BindingContext> CONTEXT = new ThreadLocal<>();

    private GatewayToolBindingContextHolder() {
    }

    public static void set(Long modelId, Long sessionId) {
        CONTEXT.set(new BindingContext(modelId, sessionId));
    }

    public static BindingContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static final class BindingContext {

        private final Long modelId;
        private final Long sessionId;

        private BindingContext(Long modelId, Long sessionId) {
            this.modelId = modelId;
            this.sessionId = sessionId;
        }

        public Long getModelId() {
            return modelId;
        }

        public Long getSessionId() {
            return sessionId;
        }
    }
}
