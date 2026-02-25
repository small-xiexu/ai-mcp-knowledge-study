package com.xbk.knowledge.config.trace;

import com.xbk.knowledge.types.trace.TraceIdUtils;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient Trace 透传定制器。
 * 为所有通过 Spring 注入的 WebClient.Builder 自动附加 X-Trace-Id 请求头。
 *
 * @author sxie
 */
@Component
public class TraceWebClientCustomizer implements WebClientCustomizer {

    /**
     * TraceId 透传请求头名称。
     */
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public void customize(WebClient.Builder webClientBuilder) {
        webClientBuilder.filter((request, next) -> {
            String traceId = TraceIdUtils.getOrCreateTraceId();
            ClientRequest tracedRequest = ClientRequest.from(request)
                    .headers(headers -> headers.set(TRACE_ID_HEADER, traceId))
                    .build();
            return next.exchange(tracedRequest);
        });
    }
}
