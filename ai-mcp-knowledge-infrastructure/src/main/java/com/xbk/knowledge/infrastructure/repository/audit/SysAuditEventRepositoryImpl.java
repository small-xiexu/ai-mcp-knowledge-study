package com.xbk.knowledge.infrastructure.repository.audit;

import com.xbk.knowledge.domain.model.entity.SysAuditEvent;
import com.xbk.knowledge.domain.model.vo.identity.AuditEventPageQuery;
import com.xbk.knowledge.domain.repository.audit.SysAuditEventRepository;
import com.xbk.knowledge.infrastructure.mapper.audit.SysAuditEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 审计事件仓储实现。
 *
 * 职责：基础设施层实现，用于落地审计事件查询。
 *
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class SysAuditEventRepositoryImpl implements SysAuditEventRepository {

    private final SysAuditEventMapper sysAuditEventMapper;

    /**
     * 写入审计事件。
     *
     * @param event 审计事件
     * @return 影响行数
     */
    @Override
    public int insert(SysAuditEvent event) {
        if (event == null) {
            return 0;
        }
        // DB 约束：operator_org_id NOT NULL。系统流程/边界情况下兜底，避免审计写入导致主流程失败。
        if (event.getOperatorOrgId() == null) {
            event.setOperatorOrgId(0L);
        }
        return sysAuditEventMapper.insertEvent(event);
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
