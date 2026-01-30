package com.xbk.knowledge.domain.service;

import com.xbk.knowledge.domain.model.entity.ConfigAudit;
import com.xbk.knowledge.domain.model.vo.audit.AuditQuery;
import com.xbk.knowledge.types.common.PageResult;

/**
 * 审计日志领域服务接口
 * 负责审计日志的业务逻辑处理
 *
 * 职责：领域服务接口，用于定义业务能力
 * @author xiexu
 */
public interface IAuditService {

    /**
     * 分页查询审计日志
     *
     * @param query 审计查询条件
     * @return 分页结果
     */
    PageResult<ConfigAudit> queryAuditPage(AuditQuery query);
}
