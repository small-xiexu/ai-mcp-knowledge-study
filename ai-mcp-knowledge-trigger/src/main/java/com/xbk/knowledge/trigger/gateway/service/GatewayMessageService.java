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
 * @author xiexu
 */
@Slf4j
@Service
public class GatewayMessageService {

    @Resource
    private Map<String, IRequestHandler> requestHandlerMap;

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
