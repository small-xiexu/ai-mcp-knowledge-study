package com.xbk.knowledge.trigger.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.domain.model.entity.gateway.McpGateway;
import com.xbk.knowledge.domain.model.entity.gateway.McpGatewayAuth;
import com.xbk.knowledge.domain.model.vo.gateway.GatewayIdQuery;
import com.xbk.knowledge.domain.repository.gateway.McpGatewayAuthRepository;
import com.xbk.knowledge.domain.repository.gateway.McpGatewayRepository;
import com.xbk.knowledge.trigger.gateway.model.McpSchemaVO;
import com.xbk.knowledge.trigger.gateway.model.SessionConfigVO;
import com.xbk.knowledge.types.exception.BusinessException;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gateway 会话服务
 *
 * 职责：管理 MCP SSE 连接的完整生命周期，包括建立连接、API Key 鉴权、
 * 速率限制、心跳保活、消息回推、会话过期清理
 *
 * @author xiexu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GatewaySessionService {

    /** 会话超时时间（分钟），超过此时间未活跃的会话将被清理 */
    private static final long SESSION_TIMEOUT_MINUTES = 30;

    private final McpGatewayRepository gatewayRepository;
    private final McpGatewayAuthRepository gatewayAuthRepository;
    private final ObjectMapper objectMapper;

    /** sessionId → 会话状态，内存维护所有活跃会话 */
    private final Map<String, SessionConfigVO> sessions = new ConcurrentHashMap<>();
    /** gatewayId:apiKey → 速率窗口，用于 API Key 调用频率限制 */
    private final Map<String, RateWindow> rateWindows = new ConcurrentHashMap<>();

    /**
     * 建立 SSE 连接
     * 流程：校验网关 → 校验 API Key → 生成 sessionId → 推送 endpoint 事件 → 启动心跳
     *
     * @param gatewayId 网关业务标识
     * @param apiKey    客户端传入的 API Key（可选，取决于网关是否配置鉴权）
     * @return SSE 事件流
     */
    public Flux<ServerSentEvent<String>> establishSseConnection(String gatewayId, String apiKey) {
        validateGateway(gatewayId);
        validateApiKey(gatewayId, apiKey);

        String sessionId = UUID.randomUUID().toString();
        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().multicast().onBackpressureBuffer();
        SessionConfigVO session = new SessionConfigVO(sessionId, sink);
        sessions.put(sessionId, session);

        String endpoint = "/api/gateway/" + gatewayId + "/mcp/message?sessionId=" + sessionId;
        sink.tryEmitNext(ServerSentEvent.<String>builder().event("endpoint").data(endpoint).build());

        Flux<ServerSentEvent<String>> heartbeat = Flux.interval(Duration.ofSeconds(30))
                .map(tick -> ServerSentEvent.<String>builder().event("ping").data("{}") .build());

        return Flux.merge(sink.asFlux(), heartbeat)
                .doOnCancel(() -> removeSession(sessionId))
                .doOnTerminate(() -> removeSession(sessionId));
    }

    /** 按 sessionId 获取活跃会话，同时刷新最后访问时间 */
    public SessionConfigVO getSession(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }
        SessionConfigVO session = sessions.get(sessionId);
        if (session == null || !session.isActive()) {
            return null;
        }
        session.updateLastAccessed();
        return session;
    }

    /** 移除会话并关闭对应的 SSE sink */
    public void removeSession(String sessionId) {
        SessionConfigVO session = sessions.remove(sessionId);
        if (session == null) {
            return;
        }
        session.markInactive();
        try {
            session.getSink().tryEmitComplete();
        } catch (Exception e) {
            log.warn("关闭 session sink 失败，sessionId: {}", sessionId, e);
        }
    }

    /** 将 JSON-RPC 响应通过 SSE 推送给客户端 */
    public void publishResponse(String sessionId, McpSchemaVO.JSONRPCResponse rpcResponse) {
        if (rpcResponse == null || !StringUtils.hasText(sessionId)) {
            return;
        }
        SessionConfigVO session = getSession(sessionId);
        if (session == null || session.getSink() == null) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(rpcResponse);
            session.getSink().tryEmitNext(ServerSentEvent.<String>builder().event("message").data(payload).build());
        } catch (Exception e) {
            log.warn("回推 JSON-RPC 响应失败，sessionId: {}", sessionId, e);
        }
    }

    /** 清理超时的过期会话（可由定时任务调用） */
    public void cleanupExpiredSessions() {
        for (Map.Entry<String, SessionConfigVO> entry : sessions.entrySet()) {
            SessionConfigVO session = entry.getValue();
            if (session == null || !session.isActive() || session.isExpired(SESSION_TIMEOUT_MINUTES)) {
                removeSession(entry.getKey());
            }
        }
    }

    /** 应用关闭时清理所有会话 */
    @PreDestroy
    public void shutdown() {
        for (String sessionId : sessions.keySet()) {
            removeSession(sessionId);
        }
    }

    /** 校验网关是否存在且已启用 */
    private void validateGateway(String gatewayId) {
        if (!StringUtils.hasText(gatewayId)) {
            throw new BusinessException("gatewayId 不能为空");
        }
        McpGateway gateway = gatewayRepository.findByGatewayId(new GatewayIdQuery(gatewayId)).orElse(null);
        if (gateway == null) {
            throw new BusinessException("网关不存在: " + gatewayId);
        }
        if (gateway.getStatus() == null || gateway.getStatus() != 1) {
            throw new BusinessException("网关未启用: " + gatewayId);
        }
    }

    /** 校验 API Key 有效性（存在性、过期、速率限制） */
    private void validateApiKey(String gatewayId, String apiKey) {
        List<McpGatewayAuth> authList = gatewayAuthRepository.findByGatewayId(new GatewayIdQuery(gatewayId));
        if (authList == null || authList.isEmpty()) {
            return;
        }

        boolean hasEnabledAuth = false;
        for (McpGatewayAuth auth : authList) {
            if (auth != null && auth.getStatus() != null && auth.getStatus() == 1) {
                hasEnabledAuth = true;
                break;
            }
        }
        if (!hasEnabledAuth) {
            return;
        }

        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException("缺少 API Key");
        }

        for (McpGatewayAuth auth : authList) {
            if (auth == null || auth.getStatus() == null || auth.getStatus() != 1) {
                continue;
            }
            if (!apiKey.equals(auth.getApiKey())) {
                continue;
            }
            if (auth.getExpireTime() != null && auth.getExpireTime().isBefore(LocalDateTime.now())) {
                throw new BusinessException("API Key 已过期");
            }
            checkRateLimit(gatewayId, apiKey, auth.getRateLimit());
            return;
        }

        throw new BusinessException("API Key 无效");
    }

    /** 基于滑动分钟窗口的速率限制检查 */
    private void checkRateLimit(String gatewayId, String apiKey, Integer rateLimit) {
        if (rateLimit == null || rateLimit <= 0) {
            return;
        }
        long minuteWindow = System.currentTimeMillis() / 60000;
        String key = gatewayId + ":" + apiKey;
        synchronized (rateWindows) {
            RateWindow window = rateWindows.get(key);
            if (window == null || window.windowMinute != minuteWindow) {
                window = new RateWindow(minuteWindow, 0);
                rateWindows.put(key, window);
            }
            if (window.count >= rateLimit) {
                throw new BusinessException("API Key 调用频率超限");
            }
            window.count++;
        }
    }

    /** 速率限制滑动窗口，按分钟粒度计数 */
    private static class RateWindow {

        private final long windowMinute;
        private int count;

        private RateWindow(long windowMinute, int count) {
            this.windowMinute = windowMinute;
            this.count = count;
        }
    }
}
