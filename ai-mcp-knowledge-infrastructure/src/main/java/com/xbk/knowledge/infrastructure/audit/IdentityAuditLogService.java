package com.xbk.knowledge.infrastructure.audit;

import com.xbk.knowledge.domain.audit.model.entity.SysAuditEvent;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.ISysAuditEventDao;
import com.xbk.knowledge.infrastructure.dao.po.SysAuditEventPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 身份域审计日志服务。
 *
 * 职责：基础设施服务，用于写入 sys_audit_event 审计记录。
 *
 * @author sxie
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityAuditLogService {

    /**
     * 审计事件数据访问对象。
     */
    private final ISysAuditEventDao sysAuditEventMapper;

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
            sysAuditEventMapper.insertEvent(BeanMappingUtils.map(event, SysAuditEventPO.class));
        } catch (Exception e) {
            log.error("写入身份审计日志失败，eventType: {}, resourceType: {}, action: {}",
                    event.getEventType(), event.getResourceType(), event.getAction(), e);
        }
    }
}
