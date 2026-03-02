package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.IMcpGatewayService;
import com.xbk.knowledge.trigger.gateway.model.McpSchemaVO;
import com.xbk.knowledge.trigger.gateway.service.GatewayMessageService;
import com.xbk.knowledge.trigger.gateway.service.GatewaySessionService;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.types.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import cn.dev33.satoken.annotation.SaCheckPermission;

import java.io.IOException;

/**
 * Gateway MCP 对外接口
 *
 * 职责：暴露 MCP 协议的 SSE 连接端点和消息处理端点，
 * 供外部 MCP 客户端（如 Claude Desktop、Cursor 等）接入
 *
 * @author sxie
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000"}, allowedHeaders = "*", maxAge = 3600)
@RequestMapping("/api/gateway")
public class McpGatewayController implements IMcpGatewayService {

    /**
     * Gateway SSE 会话服务。
     */
    private final GatewaySessionService gatewaySessionService;

    /**
     * Gateway 消息处理服务。
     */
    private final GatewayMessageService gatewayMessageService;

    /**
     * 建立 MCP SSE 长连接。
     * 流程：
     * 1. 网关客户端携带 `gatewayId` 与 API Key（Header）访问该接口。
     * 2. Controller 校验 API Key 必须通过 Header 传递。
     * 3. 调用 `gatewaySessionService.establishSseConnection` 创建会话与事件流。
     * 4. 返回 `Flux<ServerSentEvent<String>>`，持续向客户端推送 endpoint/心跳等事件。
     *
     * @param gatewayId 网关 ID。
     * @param apiKey API Key（必须通过 X-API-Key Header 传递）。
     * @param apiKeyQuery API Key 查询串（已废弃，不再支持）。
     * @return Gateway SSE 事件流。
     */
    @GetMapping(value = "/{gatewayId}/mcp/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SaCheckPermission("gateway:connect")
    @Override
    public Flux<ServerSentEvent<String>> establishSseConnection(@PathVariable("gatewayId") String gatewayId,
                                                                 @RequestHeader(value = "X-API-Key", required = false) String apiKey,
                                                                 @RequestParam(value = "apiKey", required = false) String apiKeyQuery) {
        if (!StringUtils.hasText(gatewayId)) {
            throw new BusinessException("gatewayId 不能为空");
        }

        log.info("收到 MCP SSE 连接请求，gatewayId: {}", gatewayId);

        // API Key 必须通过 Header 传递，禁止通过 Query 参数传递
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException("缺少 API Key，请在请求头中设置 X-API-Key");
        }

        Flux<ServerSentEvent<String>> flux = gatewaySessionService.establishSseConnection(gatewayId, apiKey);
        log.info("MCP SSE 连接建立成功，gatewayId: {}", gatewayId);
        return flux.doOnError(e -> log.error("SSE 连接异常，gatewayId: {}", gatewayId, e));
    }

    /**
     * 处理 MCP JSON-RPC 消息。
     * 流程：
     * 1. 客户端通过 HTTP POST 提交 JSON-RPC 消息与 `sessionId`。
     * 2. Controller 校验 `sessionId` 与 `body` 非空后反序列化为 JSON-RPC 对象。
     * 3. 调用 `gatewayMessageService.process` 执行协议路由与业务处理。
     * 4. 通过 `gatewaySessionService.publishResponse` 将响应推送回对应 SSE 会话。
     * 5. 正常返回 `Result.success`；业务异常由全局异常处理器处理。
     *
     * @param gatewayId 网关 ID。
     * @param sessionId 会话 ID。
     * @param body 请求体。
     * @return JSON-RPC 消息处理结果。
     */
    @PostMapping(value = "/{gatewayId}/mcp/message", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SaCheckPermission("tool:invoke")
    @Override
    public Mono<ResponseEntity<Object>> handleMessage(@PathVariable("gatewayId") String gatewayId,
                                                      @RequestParam("sessionId") String sessionId,
                                                      @RequestBody String body) {
        if (!StringUtils.hasText(sessionId)) {
            throw new BusinessException("sessionId 不能为空");
        }
        if (!StringUtils.hasText(body)) {
            throw new BusinessException("messageBody 不能为空");
        }

        McpSchemaVO.JSONRPCMessage message;
        try {
            message = McpSchemaVO.deserializeJsonRpcMessage(body);
        } catch (IOException e) {
            throw new BusinessException("无效的 JSON-RPC 消息格式");
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无法识别的 JSON-RPC 消息类型");
        }

        McpSchemaVO.JSONRPCResponse response = gatewayMessageService.process(gatewayId, message);
        gatewaySessionService.publishResponse(sessionId, response);
        return Mono.just(ResponseEntity.ok(Result.success(response)));
    }
}
