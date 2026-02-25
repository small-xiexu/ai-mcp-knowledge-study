package com.xbk.knowledge.trigger.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 网关会话状态
 * <p>
 * 职责：维护单个 SSE 话的运行时状态，包括 sessionId、SSE sink、创建时间、
 * 最后访问时间和活跃标记，用于话管理和过期清理
 *
 * @author sxie
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionConfigVO {

    /**
     * 话唯一标识
     */
    private String sessionId;

    /**
     * SSE 事件发射器，用于向客户端推送消息
     */
    private Sinks.Many<ServerSentEvent<String>> sink;

    /**
     * 话创建时间
     */
    private Instant createTime;

    /**
     * 最后一次访问时间（用于过期判断）
     */
    private volatile Instant lastAccessedTime;

    /**
     * 话是否活跃
     */
    private volatile boolean active;

    /**
     * 标记业务状态。
     * 
     * @param sessionId 会话 ID
     * @param sink 输出流。
     */
    public SessionConfigVO(String sessionId, Sinks.Many<ServerSentEvent<String>> sink) {
        this.sessionId = sessionId;
        this.sink = sink;
        this.createTime = Instant.now();
        this.lastAccessedTime = Instant.now();
        this.active = true;
    }

    /**
     * 标记话为非活跃状态
     */
    public void markInactive() {
        this.active = false;
    }

    /**
     * 刷新最后访问时间
     */
    public void updateLastAccessed() {
        this.lastAccessedTime = Instant.now();
    }

    /**
     * 判断话是否已超过指定分钟数未活跃
     * 
     * @param timeoutMinutes 超时时长（分钟）。
     * @return `true` 表示会话已超时，`false` 表示会话仍有效。
     */
    public boolean isExpired(long timeoutMinutes) {
        if (lastAccessedTime == null) {
            return true;
        }
        return lastAccessedTime.isBefore(Instant.now().minus(timeoutMinutes, ChronoUnit.MINUTES));
    }
}
