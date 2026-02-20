package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.audit.model.entity.SysAuditEvent;
import com.xbk.knowledge.domain.identity.model.valobj.AuditEventPageQuery;
import com.xbk.knowledge.types.common.PageResult;

/**
 * 审计事件查询应用服务接口。
 *
 * 职责：应用层用例接口，用于封装审计事件查询能力。
 *
 * @author sxie
 */
public interface AuditEventAppService {

    /**
     * 分页查询审计事件。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<SysAuditEvent> queryPage(AuditEventPageQuery query);
}
