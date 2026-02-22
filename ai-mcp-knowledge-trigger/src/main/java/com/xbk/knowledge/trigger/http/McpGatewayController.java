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
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/gateway")
public class McpGatewayController implements IMcpGatewayService {

    private final GatewaySessionService gatewaySessionService;
    private final GatewayMessageService gatewayMessageService;

    /**
     * 建立 SSE 连接
     * MCP 客户端通过此端点建立长连接，接收 endpoint 事件和心跳
     * API Key 支持 Header（X-API-Key）和 Query 参数两种传递方式
     */
    @GetMapping(value = "/{gatewayId}/mcp/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Override
    public Flux<ServerSentEvent<String>> establishSseConnection(@PathVariable("gatewayId") String gatewayId,
                                                                 @RequestHeader(value = "X-API-Key", required = false) String apiKey,
                                                                 @RequestParam(value = "apiKey", required = false) String apiKeyQuery) {
        String mergedApiKey = StringUtils.hasText(apiKey) ? apiKey : apiKeyQuery;
        return gatewaySessionService.establishSseConnection(gatewayId, mergedApiKey);
    }

    /**
     * 处理 JSON-RPC 消息
     * MCP 客户端通过此端点发送 initialize/tools/list/tools/call 等请求，
     * 响应同时通过 SSE 通道推送给客户端
     */
    @PostMapping(value = "/{gatewayId}/mcp/message", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Override
    public Mono<ResponseEntity<Object>> handleMessage(@PathVariable("gatewayId") String gatewayId,
                                                      @RequestParam("sessionId") String sessionId,
                                                      @RequestBody String body) {
        try {
            if (!StringUtils.hasText(sessionId)) {
                throw new BusinessException("sessionId 不能为空");
            }
            if (!StringUtils.hasText(body)) {
                throw new BusinessException("messageBody 不能为空");
            }
            McpSchemaVO.JSONRPCMessage message = McpSchemaVO.deserializeJsonRpcMessage(body);
            McpSchemaVO.JSONRPCResponse response = gatewayMessageService.process(gatewayId, message);
            gatewaySessionService.publishResponse(sessionId, response);
            return Mono.just(ResponseEntity.ok(Result.success(response)));
        } catch (BusinessException e) {
            return Mono.just(ResponseEntity.badRequest().body(Result.error(e.getCode(), e.getMessage())));
        } catch (Exception e) {
            log.error("处理 Gateway MCP 消息失败，gatewayId: {}, sessionId: {}", gatewayId, sessionId, e);
            return Mono.just(ResponseEntity.internalServerError().body(Result.error("处理消息失败")));
        }
    }
}
