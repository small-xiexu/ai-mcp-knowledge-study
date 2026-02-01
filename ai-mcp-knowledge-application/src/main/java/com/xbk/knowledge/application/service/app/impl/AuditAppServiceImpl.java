package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.AuditAppService;
import com.xbk.knowledge.domain.model.entity.ConfigAudit;
import com.xbk.knowledge.domain.model.vo.audit.AuditQuery;
import com.xbk.knowledge.domain.service.IAuditService;
import com.xbk.knowledge.types.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

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
     *
     * 为什么：统一审计查询入口，隔离应用层与领域层协议
     * 入参：审计查询对象
     * 出参：分页结果
     */
    @Override
    public PageResult<ConfigAudit> queryAuditPage(AuditQuery query) {
        return auditService.queryAuditPage(query);
    }

    /**
     * 查询所有可用表名
     *
     * 为什么：提供筛选维度给前端
     * 入参：无
     * 出参：表名列表
     */
    @Override
    public List<String> listTableNames() {
        return auditService.listTableNames();
    }
}
