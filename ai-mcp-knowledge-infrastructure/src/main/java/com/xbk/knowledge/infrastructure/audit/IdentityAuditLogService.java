package com.xbk.knowledge.infrastructure.audit;

import com.xbk.knowledge.domain.model.entity.SysAuditEvent;
import com.xbk.knowledge.infrastructure.mapper.audit.SysAuditEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 身份域审计日志服务。
 *
 * 职责：基础设施服务，用于写入 sys_audit_event 审计记录。
 *
 * @author xiexu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityAuditLogService {

    private final SysAuditEventMapper sysAuditEventMapper;

    /**
     * 写入审计日志。
     *
     * @param event 审计事件
     */
    public void record(SysAuditEvent event) {
        try {
            if (event.getOccurredAt() == null) {
                event.setOccurredAt(LocalDateTime.now());
            }
            sysAuditEventMapper.insertEvent(event);
        } catch (Exception e) {
            log.error("写入身份审计日志失败，eventType: {}, resourceType: {}, action: {}",
                    event.getEventType(), event.getResourceType(), event.getAction(), e);
        }
    }
}
