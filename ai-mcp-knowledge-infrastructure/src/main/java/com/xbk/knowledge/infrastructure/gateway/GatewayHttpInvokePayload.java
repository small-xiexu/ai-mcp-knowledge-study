package com.xbk.knowledge.infrastructure.gateway;

import lombok.Data;
import org.springframework.http.HttpMethod;

import java.util.Map;

/**
 * HTTP 调用载荷。
 *
 * 职责：封装工具调用阶段的 URL、方法、请求头、Query 与 Body。
 *
 * @author xiexu
 */
@Data
public class GatewayHttpInvokePayload {

    /**
     * 请求 URL。
     */
    private String url;

    /**
     * HTTP 方法。
     */
    private HttpMethod method;

    /**
     * 请求头参数。
     */
    private Map<String, String> headers;

    /**
     * Query 参数。
     */
    private Map<String, Object> query;

    /**
     * 请求体参数。
     */
    private Map<String, Object> body;
}
