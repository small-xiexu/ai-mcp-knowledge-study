package com.xbk.knowledge.application.service.impl;

import com.xbk.knowledge.application.service.AuditAppService;
import com.xbk.knowledge.domain.model.entity.ConfigAudit;
import com.xbk.knowledge.domain.model.vo.audit.AuditQuery;
import com.xbk.knowledge.domain.service.IAuditService;
import com.xbk.knowledge.types.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 审计日志应用服务实现
 * 负责审计查询用例编排
 *
 * 职责：应用层用例实现，用于协调领域能力
 * @author xiexu
 */
@Service
@RequiredArgsConstructor
public class AuditAppServiceImpl implements AuditAppService {

    private final IAuditService auditService;

    /**
     * 分页查询审计日志
     * 负责应用层用例编排，调用领域服务获取审计分页
     */
    @Override
    public PageResult<ConfigAudit> queryAuditPage(AuditQuery query) {
        return auditService.queryAuditPage(query);
    }
}
