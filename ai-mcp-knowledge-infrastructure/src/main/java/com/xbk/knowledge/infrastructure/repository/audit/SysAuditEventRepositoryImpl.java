package com.xbk.knowledge.infrastructure.repository.audit;

import com.xbk.knowledge.domain.audit.model.entity.SysAuditEvent;
import com.xbk.knowledge.domain.identity.model.valobj.AuditEventPageQuery;
import com.xbk.knowledge.domain.audit.adapter.repository.SysAuditEventRepository;
import com.xbk.knowledge.infrastructure.dao.ISysAuditEventDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 审计事件仓储实现。
 *
 * 职责：基础设施层实现，用于落地审计事件查询。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class SysAuditEventRepositoryImpl implements SysAuditEventRepository {

    private final ISysAuditEventDao sysAuditEventMapper;

    /**
     * 写入审计事件。
     *
     * @param event 审计事件
     */
    @Override
    public void insert(SysAuditEvent event) {
        if (event == null) {
            return;
        }
        // DB 约束：operator_scope_id NOT NULL。系统流程/边界情况下兜底，避免审计写入导致主流程失败。
        if (event.getOperatorScopeId() == null) {
            event.setOperatorScopeId(0L);
        }
        sysAuditEventMapper.insertEvent(event);
    }

    /**
     * 分页查询审计事件。
     *
     * @param query 查询条件
     * @return 审计事件列表
     */
    @Override
    public List<SysAuditEvent> findPage(AuditEventPageQuery query) {
        return sysAuditEventMapper.findPage(query);
    }

    /**
     * 统计审计事件总数。
     *
     * @param query 查询条件
     * @return 总数
     */
    @Override
    public long count(AuditEventPageQuery query) {
        return sysAuditEventMapper.count(query);
    }
}
