package com.xbk.knowledge.types.trace;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 TraceId 生成与 MDC 写回逻辑，避免链路信息丢失。
 *
 * @author xiexu
 */
public class TraceIdUtilsTest {

    /**
     * 对外暴露 shouldGenerateTraceIdWhenMissing 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldGenerateTraceIdWhenMissing() {
        MDC.remove(TraceIdUtils.TRACE_ID_KEY);

        TraceIdUtils.TraceIdContext context = TraceIdUtils.ensureTraceId();

        assertTrue(context.isGenerated());
        assertNotNull(context.getTraceId());
        assertEquals(16, context.getTraceId().length());
        assertEquals(context.getTraceId(), MDC.get(TraceIdUtils.TRACE_ID_KEY));

        TraceIdUtils.clearIfGenerated(context.isGenerated());
        assertNull(MDC.get(TraceIdUtils.TRACE_ID_KEY));
    }

    /**
     * 对外暴露 shouldReuseExistingTraceId 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldReuseExistingTraceId() {
        MDC.put(TraceIdUtils.TRACE_ID_KEY, "trace-123");

        TraceIdUtils.TraceIdContext context = TraceIdUtils.ensureTraceId();

        assertFalse(context.isGenerated());
        assertEquals("trace-123", context.getTraceId());
        TraceIdUtils.clear();
    }
}
