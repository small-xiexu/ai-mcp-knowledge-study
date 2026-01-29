package com.xbk.knowledge.types.trace;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * TraceId 工具类
 * 统一生成与管理 traceId，避免重复实现并保证写回 MDC。
 *
 * 职责：链路追踪工具，用于统一 traceId 处理
 * @author xiexu
 * @since 2026-01-27
 */
public final class TraceIdUtils {

    public static final String TRACE_ID_KEY = "traceId";

    private TraceIdUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 确保 MDC 中存在 traceId，便于链路日志统一关联。
     *
     * @return TraceIdContext
     */
    public static TraceIdContext ensureTraceId() {
        String currentTraceId = MDC.get(TRACE_ID_KEY);
        if (currentTraceId == null || currentTraceId.isEmpty()) {
            String traceId = generateTraceId();
            MDC.put(TRACE_ID_KEY, traceId);
            return new TraceIdContext(traceId, true);
        }
        return new TraceIdContext(currentTraceId, false);
    }

    /**
     * 获取或生成 traceId，并写回 MDC。
     *
     * @return traceId
     */
    public static String getOrCreateTraceId() {
        return ensureTraceId().getTraceId();
    }

    /**
     * 仅在本次调用生成了 traceId 时清理 MDC，避免污染线程复用环境。
     *
     * @param generated 是否为本次生成
     */
    public static void clearIfGenerated(boolean generated) {
        if (generated) {
            MDC.remove(TRACE_ID_KEY);
        }
    }

    /**
     * 清理 MDC 中的 traceId，用于显式结束链路。
     */
    public static void clear() {
        MDC.remove(TRACE_ID_KEY);
    }

    /**
     * 生成 16 位 traceId。
     *
     * @return traceId
     */
    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * TraceId 上下文，用于区分是否由当前调用生成。
     *
     * @param traceId   traceId
     * @param generated 是否本次生成
     */
    @Getter
    @AllArgsConstructor
    public static class TraceIdContext {

        /**
         * traceId
         */
        private final String traceId;

        /**
         * 是否本次生成
         */
        private final boolean generated;
    }
}
