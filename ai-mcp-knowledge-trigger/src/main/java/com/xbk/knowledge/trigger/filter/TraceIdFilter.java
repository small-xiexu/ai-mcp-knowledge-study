package com.xbk.knowledge.trigger.filter;

import com.xbk.knowledge.types.trace.TraceIdUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * HTTP TraceId 过滤器
 * 统一为每个请求注入 traceId 并回写响应头，保证日志可串联
 *
 * 职责：触发层基础设施，用于请求级链路追踪
 * @author sxie
 */
@Slf4j
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String previous = MDC.get(TraceIdUtils.TRACE_ID_KEY);
        String incoming = request.getHeader(TRACE_ID_HEADER);
        String traceId = StringUtils.hasText(incoming) ? incoming : TraceIdUtils.getOrCreateTraceId();
        /*
         * 目的：优先使用上游传入的 traceId，保证跨服务链路一致
 */
        MDC.put(TraceIdUtils.TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            /*
             * 目的：恢复或清理 MDC，避免线程复用导致串号
 */
            if (StringUtils.hasText(previous)) {
                MDC.put(TraceIdUtils.TRACE_ID_KEY, previous);
            } else {
                MDC.remove(TraceIdUtils.TRACE_ID_KEY);
            }
        }
    }
}
