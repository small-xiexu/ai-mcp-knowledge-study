package com.xbk.knowledge.domain.service.audit;

import com.xbk.knowledge.domain.audit.model.entity.ConfigAudit;
import com.xbk.knowledge.domain.audit.model.valobj.AuditQuery;
import com.xbk.knowledge.types.common.PageResult;
import java.util.List;

/**
 * 审计日志领域服务接口
 * 负责审计日志的业务逻辑处理
 *
 * 职责：领域服务接口，用于定义业务能力
 * @author sxie
 */
public interface IAuditService {

    /**
     * 分页查询审计日志
     *
     * 为什么：统一审计查询入口，便于扩展筛选逻辑
     * 入参：审计查询条件
     * 出参：分页结果
     */
    PageResult<ConfigAudit> queryAuditPage(AuditQuery query);

    /**
     * 查询所有可用表名
     *
     * 为什么：提供筛选下拉数据源
     * 入参：无
     * 出参：表名列表
     */
    List<String> listTableNames();
}
