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
 *
 * @author xiexu
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionConfigVO {

    private String sessionId;

    private Sinks.Many<ServerSentEvent<String>> sink;

    private Instant createTime;

    private volatile Instant lastAccessedTime;

    private volatile boolean active;

    public SessionConfigVO(String sessionId, Sinks.Many<ServerSentEvent<String>> sink) {
        this.sessionId = sessionId;
        this.sink = sink;
        this.createTime = Instant.now();
        this.lastAccessedTime = Instant.now();
        this.active = true;
    }

    public void markInactive() {
        this.active = false;
    }

    public void updateLastAccessed() {
        this.lastAccessedTime = Instant.now();
    }

    public boolean isExpired(long timeoutMinutes) {
        if (lastAccessedTime == null) {
            return true;
        }
        return lastAccessedTime.isBefore(Instant.now().minus(timeoutMinutes, ChronoUnit.MINUTES));
    }
}
