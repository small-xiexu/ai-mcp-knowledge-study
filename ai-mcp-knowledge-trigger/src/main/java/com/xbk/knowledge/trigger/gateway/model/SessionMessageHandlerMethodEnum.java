package com.xbk.knowledge.trigger.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Gateway JSON-RPC 方法枚举
 *
 * 职责：定义 MCP 协议支持的 JSON-RPC 方法与对应 Handler Bean 名称的映射关系
 *
 * @author sxie
 */
@Getter
@AllArgsConstructor
public enum SessionMessageHandlerMethodEnum {

    INITIALIZE("initialize", "gatewayInitializeHandler"),       // MCP 握手
    TOOLS_LIST("tools/list", "gatewayToolsListHandler"),        // 工具清单查询
    TOOLS_CALL("tools/call", "gatewayToolsCallHandler");        // 工具调用执行

    /**
     * JSON-RPC method 字段值
     */
     private final String method;
    /**
     * 对应的 Spring Bean 名称
     */
     private final String handlerName;

    private static final Map<String, SessionMessageHandlerMethodEnum> METHOD_MAP;

    static {
        Map<String, SessionMessageHandlerMethodEnum> map = new HashMap<>();
        for (SessionMessageHandlerMethodEnum value : values()) {
            map.put(value.getMethod(), value);
        }
        METHOD_MAP = Collections.unmodifiableMap(map);
    }

    /**
     * 按 method 字符串查找对应枚举值，未匹配返回 null
     */
     public static SessionMessageHandlerMethodEnum getByMethod(String method) {
        if (method == null) {
            return null;
        }
        return METHOD_MAP.get(method);
    }
}
