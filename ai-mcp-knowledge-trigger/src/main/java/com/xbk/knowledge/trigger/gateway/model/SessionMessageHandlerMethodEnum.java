package com.xbk.knowledge.trigger.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Gateway JSON-RPC 方法枚举
 *
 * @author xiexu
 */
@Getter
@AllArgsConstructor
public enum SessionMessageHandlerMethodEnum {

    INITIALIZE("initialize", "gatewayInitializeHandler"),
    TOOLS_LIST("tools/list", "gatewayToolsListHandler"),
    TOOLS_CALL("tools/call", "gatewayToolsCallHandler");

    private final String method;
    private final String handlerName;

    private static final Map<String, SessionMessageHandlerMethodEnum> METHOD_MAP;

    static {
        Map<String, SessionMessageHandlerMethodEnum> map = new HashMap<>();
        for (SessionMessageHandlerMethodEnum value : values()) {
            map.put(value.getMethod(), value);
        }
        METHOD_MAP = Collections.unmodifiableMap(map);
    }

    public static SessionMessageHandlerMethodEnum getByMethod(String method) {
        if (method == null) {
            return null;
        }
        return METHOD_MAP.get(method);
    }
}
