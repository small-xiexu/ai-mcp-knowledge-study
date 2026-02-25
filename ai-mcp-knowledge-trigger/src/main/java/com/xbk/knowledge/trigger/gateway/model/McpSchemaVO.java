package com.xbk.knowledge.trigger.gateway.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.types.json.JsonMapUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.Map;

/**
 * Gateway MCP 协议模型
 *
 * 职责：定义 JSON-RPC 2.0 消息结构，包括 Request、Notification、Response 三种消息类型，
 * 以及消息的序列化/反序列化工具方法
 *
 * @author sxie
 */
public final class McpSchemaVO {

    /**
     * JSON-RPC 协议版本号。
     */
    public static final String JSONRPC_VERSION = "2.0";

    /**
     * JSON 序列化/反序列化组件。
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private McpSchemaVO() {
    }

    /**
     * 将 JSON 文本反序列化为对应的 JSON-RPC 消息类型
     * 根据字段特征自动判断有 method + id → Request，仅 method → Notification，有 result/error → Response
     * 
     * @param jsonText JSON 文本。
     * @return 反序列化后的 JSON-RPC 消息。
     */
    public static JSONRPCMessage deserializeJsonRpcMessage(String jsonText) throws IOException {
        Map<String, Object> map = JsonMapUtils.readMap(OBJECT_MAPPER, jsonText);
        if (map.containsKey("method") && map.containsKey("id")) {
            return OBJECT_MAPPER.convertValue(map, JSONRPCRequest.class);
        }
        if (map.containsKey("method")) {
            return OBJECT_MAPPER.convertValue(map, JSONRPCNotification.class);
        }
        if (map.containsKey("result") || map.containsKey("error")) {
            return OBJECT_MAPPER.convertValue(map, JSONRPCResponse.class);
        }
        throw new IllegalArgumentException("无法识别 JSON-RPC 消息类型");
    }

    /**
     * 将 data 对象转换为指定类型（用于解析 params 字段）
     * 
     * @param data 待转换的数据。
     * @param typeRef 目标类型引用。
     * @return 方法执行结果。
     */
    public static <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
        return OBJECT_MAPPER.convertValue(data, typeRef);
    }

    /**
     * JSON-RPC 消息基础接口。
     */
    public interface JSONRPCMessage {

        String getJsonrpc();
    }

    /**
     * JSON-RPC 请求消息（包含 method + id，需要响应）。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JSONRPCRequest implements JSONRPCMessage {

        /**
         * 协议版本。
         */
        @JsonProperty("jsonrpc")
        private String jsonrpc;

        /**
         * 请求方法名。
         */
        @JsonProperty("method")
        private String method;

        /**
         * 请求 ID。
         */
        @JsonProperty("id")
        private Object id;

        /**
         * 请求参数。
         */
        @JsonProperty("params")
        private Object params;
    }

    /**
     * JSON-RPC 通知消息（仅包含 method，无需响应）。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JSONRPCNotification implements JSONRPCMessage {

        /**
         * 协议版本。
         */
        @JsonProperty("jsonrpc")
        private String jsonrpc;

        /**
         * 通知方法名。
         */
        @JsonProperty("method")
        private String method;

        /**
         * 通知参数。
         */
        @JsonProperty("params")
        private Object params;
    }

    /**
     * JSON-RPC 响应消息（包含 result 或 error）。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JSONRPCResponse implements JSONRPCMessage {

        /**
         * 协议版本。
         */
        @JsonProperty("jsonrpc")
        private String jsonrpc;

        /**
         * 请求 ID。
         */
        @JsonProperty("id")
        private Object id;

        /**
         * 响应结果。
         */
        @JsonProperty("result")
        private Object result;

        /**
         * 响应错误对象。
         */
        @JsonProperty("error")
        private JSONRPCError error;

        /**
         * JSON-RPC 错误对象。
         */
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class JSONRPCError {

            /**
             * 错误码。
             */
            @JsonProperty("code")
            private int code;

            /**
             * 错误信息。
             */
            @JsonProperty("message")
            private String message;

            /**
             * 错误附加数据。
             */
            @JsonProperty("data")
            private Object data;
        }
    }

    /**
     * MCP initialize 请求参数。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InitializeRequest {

        /**
         * 客户端声明的协议版本。
         */
        @JsonProperty("protocolVersion")
        private String protocolVersion;
    }
}
