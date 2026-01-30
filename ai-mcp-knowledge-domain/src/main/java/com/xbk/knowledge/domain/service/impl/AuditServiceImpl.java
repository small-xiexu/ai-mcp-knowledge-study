package com.xbk.knowledge.domain.service.impl;

import com.xbk.knowledge.domain.model.entity.ConfigAudit;
import com.xbk.knowledge.domain.model.vo.AuditQuery;
import com.xbk.knowledge.domain.repository.ConfigAuditRepository;
import com.xbk.knowledge.domain.service.IAuditService;
import com.xbk.knowledge.types.common.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 审计日志领域服务实现
 * 封装审计日志的业务逻辑
 *
 * 职责：领域服务实现，用于封装业务规则
 * @author xiexu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements IAuditService {

    private final ConfigAuditRepository configAuditRepository;

    /**
     * 分页查询审计日志
     * 统一过滤与排序规则，避免 SQL 注入风险
     */
    @Override
    public PageResult<ConfigAudit> queryAuditPage(AuditQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("审计查询条件不能为空");
        }
        // 解析排序字段和排序方向（白名单方式避免 SQL 注入）
        String sortColumn = resolveSortColumn(query.getSortField());
        String resolvedSortOrder = resolveSortOrder(query.getSortOrder());

        // 规范化查询条件
        String normalizedTableName = normalizeText(query.getTableName());
        String normalizedOperator = normalizeText(query.getOperator());

        // 查询分页数据
        AuditQuery normalizedQuery = new AuditQuery(
                normalizedTableName,
                query.getRecordId(),
                normalizedOperator,
                query.getOffset(),
                query.getPageSize(),
                sortColumn,
                resolvedSortOrder
        );
        List<ConfigAudit> audits = configAuditRepository.findByConditions(normalizedQuery);

        // 查询总数
        long total = configAuditRepository.countByConditions(normalizedQuery);

        // 计算页码
        int pageNum = (query.getOffset() / query.getPageSize()) + 1;

        return PageResult.of(audits, total, pageNum, query.getPageSize());
    }

    /**
     * 规范化文本
     * 统一去空白并处理空值
     */
    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 解析排序字段
     * 使用白名单映射，避免非法字段注入
     */
    private String resolveSortColumn(String sortField) {
        if (!StringUtils.hasText(sortField)) {
            return "created_at";
        }
        return switch (sortField.trim()) {
            case "id" -> "id";
            case "tableName" -> "table_name";
            case "recordId" -> "record_id";
            case "operator" -> "operator";
            case "operation" -> "operation";
            case "createdAt" -> "created_at";
            default -> "created_at";
        };
    }

    /**
     * 解析排序方向
     * 仅允许 ASC/DESC，其余回退默认值
     */
    private String resolveSortOrder(String sortOrder) {
        if (!StringUtils.hasText(sortOrder)) {
            return "DESC";
        }
        String normalized = sortOrder.trim().toUpperCase();
        return (normalized.equals("ASC") || normalized.equals("DESC")) ? normalized : "DESC";
    }
}
