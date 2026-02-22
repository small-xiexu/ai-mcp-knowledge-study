package com.xbk.knowledge.config.trace;

import com.xbk.knowledge.types.trace.TraceIdUtils;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * MDC 任务装饰器。
 * 用于在线程切换时透传 traceId，避免异步日志断链。
 *
 * @author sxie
 */
@Component
public class TraceMdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> parentContext = MDC.getCopyOfContextMap();
        String parentTraceId = MDC.get(TraceIdUtils.TRACE_ID_KEY);
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            try {
                if (parentContext != null) {
                    MDC.setContextMap(parentContext);
                } else {
                    MDC.clear();
                }
                if (StringUtils.hasText(parentTraceId)) {
                    MDC.put(TraceIdUtils.TRACE_ID_KEY, parentTraceId);
                } else {
                    TraceIdUtils.ensureTraceId();
                }
                runnable.run();
            } finally {
                if (previous != null) {
                    MDC.setContextMap(previous);
                } else {
                    MDC.clear();
                }
            }
        };
    }
}
