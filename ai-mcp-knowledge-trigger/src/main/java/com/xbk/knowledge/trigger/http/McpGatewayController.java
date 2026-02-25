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

    /**
     * Gateway SSE 话服务。
     */
    private final GatewaySessionService gatewaySessionService;

    /**
     * Gateway 消息处理服务。
     */
    private final GatewayMessageService gatewayMessageService;

    /**
     * 建立 MCP SSE 长连接。
     * 流程：
     * 1. 网关客户端携带 `gatewayId` 与 API Key（Header 或 Query）访问该接口。
     * 2. Controller 合并两种 API Key 传参来源，得到最终鉴权参数。
     * 3. 调用 `gatewaySessionService.establishSseConnection` 创建会话与事件流。
     * 4. 返回 `Flux<ServerSentEvent<String>>`，持续向客户端推送 endpoint/心跳等事件。
     * 
     * @param gatewayId 标识 ID。
     * @param apiKey API Key。
     * @param apiKeyQuery API Key 查询串。
     * @return Gateway SSE 事件流。
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
     * 处理 MCP JSON-RPC 消息。
     * 流程：
     * 1. 客户端通过 HTTP POST 提交 JSON-RPC 消息与 `sessionId`。
     * 2. Controller 校验 `sessionId` 与 `body` 非空后反序列化为 JSON-RPC 对象。
     * 3. 调用 `gatewayMessageService.process` 执行协议路由与业务处理。
     * 4. 通过 `gatewaySessionService.publishResponse` 将响应推送回对应 SSE 话。
     * 5. 正常返回 `Result.success`；业务异常返回 400；系统异常返回 500。
     * 
     * @param gatewayId 标识 ID。
     * @param sessionId 会话 ID。
     * @param body 请求体。
     * @return JSON-RPC 消息处理结果。
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
