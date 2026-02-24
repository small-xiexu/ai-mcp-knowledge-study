package com.xbk.knowledge.infrastructure.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.domain.audit.model.entity.SysAuditEvent;
import com.xbk.knowledge.domain.audit.adapter.repository.SysAuditEventRepository;
import com.xbk.knowledge.types.trace.TraceIdUtils;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 配置审计服务
 * 将配置变更统一写入 sys_audit_event
 *
 * 职责：基础设施审计能力，用于持久化变更记录
 * @author sxie
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuditService {

    private static final String EVENT_TYPE = "CONFIG_CHANGE";

    private final SysAuditEventRepository sysAuditEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * 记录审计日志
     * 将写入流程集中在服务层，便于统一事务与序列化策略
     *
     * 为什么：统一审计写入与序列化策略
     * 入参：表名、记录ID、操作类型、旧值、新值
     * 出参：无
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordAudit(String tableName, Long recordId, String operation, Object oldValue, Object newValue) {
        if (recordId == null) {
            log.warn("审计日志未记录，记录ID为空，tableName: {}, operation: {}", tableName, operation);
            return;
        }

        // 统一解析操作者并序列化数据
        Long operatorId = resolveOperatorId();
        String operatorType = operatorId == null ? "system" : "user";
        String oldValueJson = toJson(oldValue);
        String newValueJson = toJson(newValue);
        SysAuditEvent event = SysAuditEvent.builder()
                .operatorId(operatorId)
                .operatorType(operatorType)
                .eventType(EVENT_TYPE)
                .resourceType(tableName)
                .resourceId(String.valueOf(recordId))
                .action(operation)
                .requestId(TraceIdUtils.getOrCreateTraceId())
                .sourceIp(resolveSourceIp())
                .userAgent(resolveUserAgent())
                .oldValue(oldValueJson)
                .newValue(newValueJson)
                .result(1)
                .costMs(0L)
                .occurredAt(LocalDateTime.now())
                .build();
        sysAuditEventRepository.insert(event);
        log.info("审计日志已记录，tableName: {}, recordId: {}, operation: {}, operatorId: {}", tableName, recordId, operation, operatorId);
    }

    /**
     * 解析当前登录用户 ID。
     */
    private Long resolveOperatorId() {
        try {
            if (!StpUtil.isLogin()) {
                return null;
            }
            Object loginId = StpUtil.getLoginId();
            return loginId == null ? null : Long.valueOf(String.valueOf(loginId));
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveSourceIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(forwardedFor)) {
            return request.getRemoteAddr();
        }
        String[] values = forwardedFor.split(",");
        return values.length == 0 ? request.getRemoteAddr() : values[0].trim();
    }

    private String resolveUserAgent() {
        HttpServletRequest request = currentRequest();
        return request == null ? null : request.getHeader("User-Agent");
    }

    /**
     * 获取当前 HTTP 请求上下文。
     *
     * @return 返回HttpServletRequest对象。
     */
    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    /**
     * 序列化为 JSON
     *
     * 为什么：审计记录统一存储 JSON 文本
     */
    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.error("序列化审计对象失败", ex);
            return null;
        }
    }
}
