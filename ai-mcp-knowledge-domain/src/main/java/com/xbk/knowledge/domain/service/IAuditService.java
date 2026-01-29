package com.xbk.knowledge.domain.service;

import com.xbk.knowledge.domain.model.entity.ConfigAudit;
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
     * @param tableName 表名（可选）
     * @param recordId  记录 ID（可选）
     * @param operator  操作人（可选）
     * @param offset    偏移量
     * @param pageSize  每页大小
     * @param sortField 排序字段
     * @param sortOrder 排序方向
     * @return 分页结果
     */
    PageResult<ConfigAudit> queryAuditPage(
            String tableName,
            Long recordId,
            String operator,
            int offset,
            int pageSize,
            String sortField,
            String sortOrder
    );
}
