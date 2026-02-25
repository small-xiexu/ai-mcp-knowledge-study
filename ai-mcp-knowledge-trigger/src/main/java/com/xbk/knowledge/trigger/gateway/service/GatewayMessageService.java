package com.xbk.knowledge.trigger.gateway.service;

import com.xbk.knowledge.trigger.gateway.handler.IRequestHandler;
import com.xbk.knowledge.trigger.gateway.model.McpSchemaVO;
import com.xbk.knowledge.trigger.gateway.model.SessionMessageHandlerMethodEnum;
import com.xbk.knowledge.types.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Gateway 消息路由服务
 *
 * 职责：接收 JSON-RPC 消息，根据 method 字段分发到对应的 Handler 处理
 * 支持的消息类型Request（需要响应）、Notification（仅记录）、Response（忽略）
 *
 * @author sxie
 */
@Slf4j
@Service
public class GatewayMessageService {

    /**
     * Handler 注册表，key 为 Bean 名称，由 Spring 自动注入
     */
     @Resource
    private Map<String, IRequestHandler> requestHandlerMap;

    /**
     * 处理 JSON-RPC 消息
     * Response 类型直接忽略，Notification 仅记录日志，Request 路由到对应 Handler
     * 
     * @param gatewayId 网关业务标识
     * @param message 反序列化后的 JSON-RPC 消息
     * @return JSON-RPC 响应（Notification 返回 null）
     */
    public McpSchemaVO.JSONRPCResponse process(String gatewayId, McpSchemaVO.JSONRPCMessage message) {
        if (message instanceof McpSchemaVO.JSONRPCResponse) {
            return null;
        }

        if (message instanceof McpSchemaVO.JSONRPCNotification notification) {
            log.info("收到通知消息，gatewayId: {}, method: {}", gatewayId, notification.getMethod());
            return null;
        }

        if (message instanceof McpSchemaVO.JSONRPCRequest request) {
            SessionMessageHandlerMethodEnum methodEnum = SessionMessageHandlerMethodEnum.getByMethod(request.getMethod());
            if (methodEnum == null) {
                throw new BusinessException("不支持的方法: " + request.getMethod());
            }
            IRequestHandler handler = requestHandlerMap.get(methodEnum.getHandlerName());
            if (handler == null) {
                throw new BusinessException("处理器不存在: " + methodEnum.getHandlerName());
            }
            return handler.handle(gatewayId, request);
        }

        throw new BusinessException("无法处理的消息类型");
    }
}
