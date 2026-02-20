package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.audit.model.entity.ConfigAudit;
import com.xbk.knowledge.domain.audit.model.valobj.AuditQuery;
import com.xbk.knowledge.types.common.PageResult;
import java.util.List;

/**
 * 审计日志应用服务接口
 * 负责审计日志查询的用例编排
 *
 * 职责：应用层用例接口，用于封装调用入口
 * @author sxie
 */
public interface AuditAppService {

    /**
     * 分页查询审计日志
     *
     * @param query 审计查询条件
     * @return 分页结果
     */
    PageResult<ConfigAudit> queryAuditPage(AuditQuery query);

    /**
     * 查询所有可用表名
     *
     * @return 表名列表
     */
    List<String> listTableNames();
}
