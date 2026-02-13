package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.AuditEventAppService;
import com.xbk.knowledge.domain.model.entity.SysAuditEvent;
import com.xbk.knowledge.domain.model.vo.identity.AuditEventPageQuery;
import com.xbk.knowledge.domain.repository.SysAuditEventRepository;
import com.xbk.knowledge.types.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 审计事件查询应用服务实现。
 *
 * 职责：应用层用例实现，用于编排审计查询流程。
 *
 * @author xiexu
 */
@Service
@RequiredArgsConstructor
public class AuditEventAppServiceImpl implements AuditEventAppService {

    private final SysAuditEventRepository sysAuditEventRepository;

    /**
     * 分页查询审计事件。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<SysAuditEvent> queryPage(AuditEventPageQuery query) {
        Integer offset = query.getOffset() == null ? 0 : query.getOffset();
        Integer pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        AuditEventPageQuery normalizedQuery = new AuditEventPageQuery(
                query.getTenantId(),
                query.getOperatorId(),
                query.getEventType(),
                query.getResourceType(),
                query.getResult(),
                offset,
                pageSize
        );
        List<SysAuditEvent> records = sysAuditEventRepository.findPage(normalizedQuery);
        long total = sysAuditEventRepository.count(normalizedQuery);
        int pageNum = (offset / pageSize) + 1;
        return PageResult.of(records, total, pageNum, pageSize);
    }
}
