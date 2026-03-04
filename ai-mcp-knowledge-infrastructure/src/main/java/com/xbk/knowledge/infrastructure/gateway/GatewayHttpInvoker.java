package com.xbk.knowledge.infrastructure.gateway;

import com.xbk.knowledge.types.trace.TraceIdUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Gateway HTTP 调用执行器。
 *
 * 职责：执行工具 HTTP 请求，统一处理重试、超时、错误转换与请求头注入。
 *
 * @author xiexu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayHttpInvoker {

    /**
     * 默认请求超时时间（毫秒）。
     */
    private static final int DEFAULT_TIMEOUT_MS = 30000;

    /**
     * 默认重试次数。
     */
    private static final int DEFAULT_RETRY_TIMES = 0;

    /**
     * WebClient 构建器。
     */
    private final WebClient.Builder webClientBuilder;

    /**
     * WebClient 实例。
     */
    private WebClient webClient;

    /**
     * 初始化 WebClient。
     */
    @PostConstruct
    public void initWebClient() {
        this.webClient = webClientBuilder.build();
    }

    /**
     * 带重试执行 HTTP 调用。
     *
     * @param payload    HTTP 调用载荷。
     * @param retryTimes 重试次数。
     * @param timeout    超时时间。
     * @return 响应文本。
     */
    public String executeWithRetry(GatewayHttpInvokePayload payload, Integer retryTimes, Integer timeout) {
        int attempts = normalizeRetryTimes(retryTimes) + 1;
        int timeoutMs = normalizeTimeout(timeout);
        Exception lastException = null;

        for (int i = 1; i <= attempts; i++) {
            try {
                return executeOnce(payload, timeoutMs);
            } catch (Exception e) {
                lastException = e;
                if (i < attempts) {
                    log.warn("HTTP 工具调用失败，准备重试 {}/{}，url: {}，原因: {}", i, attempts, payload.getUrl(), e.getMessage());
                }
            }
        }
        throw new IllegalStateException("HTTP 工具调用失败", lastException);
    }

    /**
     * 执行单次 HTTP 请求。
     *
     * @param payload   HTTP 调用载荷。
     * @param timeoutMs 超时毫秒。
     * @return 响应文本。
     */
    private String executeOnce(GatewayHttpInvokePayload payload, int timeoutMs) {
        String finalUrl = buildFinalUrl(payload.getUrl(), payload.getQuery());
        WebClient.RequestHeadersSpec<?> requestHeadersSpec = buildRequestSpec(payload, finalUrl);
        return executeRequestAndReadBody(requestHeadersSpec, timeoutMs);
    }

    /**
     * 构建可执行的 WebClient 请求规格。
     *
     * @param payload  HTTP 调用载荷。
     * @param finalUrl 最终 URL。
     * @return 请求规格。
     */
    private WebClient.RequestHeadersSpec<?> buildRequestSpec(GatewayHttpInvokePayload payload, String finalUrl) {
        WebClient.RequestBodySpec request = webClient
                .method(payload.getMethod())
                .uri(finalUrl)
                .header("X-Trace-Id", TraceIdUtils.getOrCreateTraceId())
                .headers(headers -> applyHeaders(headers, payload.getHeaders()));
        if (!supportsBody(payload.getMethod())) {
            return request;
        }
        return request
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload.getBody());
    }

    /**
     * 执行请求并读取响应文本。
     *
     * @param requestHeadersSpec 请求规格。
     * @param timeoutMs          超时毫秒。
     * @return 响应文本。
     */
    private String executeRequestAndReadBody(WebClient.RequestHeadersSpec<?> requestHeadersSpec, int timeoutMs) {
        return requestHeadersSpec
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::buildHttpError)
                .bodyToMono(String.class)
                .defaultIfEmpty("")
                .timeout(Duration.ofMillis(timeoutMs))
                .block();
    }

    /**
     * 将非 2xx 响应转换为统一异常。
     *
     * @param response WebClient 响应对象。
     * @return 异常 Mono。
     */
    private Mono<? extends Throwable> buildHttpError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> Mono.error(new IllegalStateException(
                        "HTTP 调用失败，status=" + response.statusCode().value() + ", body=" + body
                )));
    }

    /**
     * 将 query 参数拼接到 URL 上。
     *
     * @param url   URL 地址。
     * @param query 查询参数集合。
     * @return 最终 URL。
     */
    private String buildFinalUrl(String url, Map<String, Object> query) {
        if (query == null || query.isEmpty()) {
            return url;
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        for (Map.Entry<String, Object> entry : query.entrySet()) {
            if (!StringUtils.hasText(entry.getKey()) || entry.getValue() == null) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof List<?> list) {
                for (Object item : list) {
                    builder.queryParam(entry.getKey(), item);
                }
                continue;
            }
            builder.queryParam(entry.getKey(), value);
        }
        return builder.build(true).toUriString();
    }

    /**
     * 合并并应用 HTTP 请求头。
     *
     * @param headers       请求头集合。
     * @param sourceHeaders 原始请求头映射。
     */
    private void applyHeaders(HttpHeaders headers, Map<String, String> sourceHeaders) {
        if (headers == null || sourceHeaders == null || sourceHeaders.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : sourceHeaders.entrySet()) {
            if (StringUtils.hasText(entry.getKey()) && entry.getValue() != null) {
                headers.add(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * 判断该 HTTP 方法是否支持请求体。
     *
     * @param method HTTP 方法。
     * @return true 表示支持 body。
     */
    private boolean supportsBody(HttpMethod method) {
        return HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method) || HttpMethod.PATCH.equals(method);
    }

    /**
     * 归一化重试次数。
     *
     * @param retryTimes 重试次数。
     * @return 归一化后的重试次数。
     */
    private int normalizeRetryTimes(Integer retryTimes) {
        if (retryTimes == null || retryTimes < 0) {
            return DEFAULT_RETRY_TIMES;
        }
        return retryTimes;
    }

    /**
     * 归一化超时。
     *
     * @param timeout 超时时间。
     * @return 归一化后的超时时间（毫秒）。
     */
    private int normalizeTimeout(Integer timeout) {
        if (timeout == null || timeout <= 0) {
            return DEFAULT_TIMEOUT_MS;
        }
        return timeout;
    }
}
