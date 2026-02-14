package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.entity.SysAuditEvent;
import com.xbk.knowledge.domain.model.vo.identity.AuditEventPageQuery;

import java.util.List;

/**
 * 审计事件仓储接口。
 *
 * 职责：审计事件数据访问抽象。
 *
 * @author xiexu
 */
public interface SysAuditEventRepository {

    /**
     * 写入审计事件。
     *
     * @param event 审计事件
     * @return 影响行数
     */
    int insert(SysAuditEvent event);

    /**
     * 分页查询审计事件。
     *
     * @param query 查询条件
     * @return 审计事件列表
     */
    List<SysAuditEvent> findPage(AuditEventPageQuery query);

    /**
     * 统计审计事件总数。
     *
     * @param query 查询条件
     * @return 总数
     */
    long count(AuditEventPageQuery query);
}
