package com.xbk.knowledge.trigger.gateway.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Gateway MCP 协议模型
 *
 * @author xiexu
 */
public final class McpSchemaVO {

    public static final String JSONRPC_VERSION = "2.0";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private McpSchemaVO() {
    }

    public static JSONRPCMessage deserializeJsonRpcMessage(String jsonText) throws IOException {
        Map<String, Object> map = OBJECT_MAPPER.readValue(jsonText, new TypeReference<HashMap<String, Object>>() {
        });
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

    public static <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
        return OBJECT_MAPPER.convertValue(data, typeRef);
    }

    public interface JSONRPCMessage {

        String getJsonrpc();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JSONRPCRequest implements JSONRPCMessage {

        @JsonProperty("jsonrpc")
        private String jsonrpc;

        @JsonProperty("method")
        private String method;

        @JsonProperty("id")
        private Object id;

        @JsonProperty("params")
        private Object params;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JSONRPCNotification implements JSONRPCMessage {

        @JsonProperty("jsonrpc")
        private String jsonrpc;

        @JsonProperty("method")
        private String method;

        @JsonProperty("params")
        private Object params;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JSONRPCResponse implements JSONRPCMessage {

        @JsonProperty("jsonrpc")
        private String jsonrpc;

        @JsonProperty("id")
        private Object id;

        @JsonProperty("result")
        private Object result;

        @JsonProperty("error")
        private JSONRPCError error;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class JSONRPCError {

            @JsonProperty("code")
            private int code;

            @JsonProperty("message")
            private String message;

            @JsonProperty("data")
            private Object data;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InitializeRequest {

        @JsonProperty("protocolVersion")
        private String protocolVersion;
    }
}
