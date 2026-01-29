package com.xbk.knowledge.domain.service.impl;

import com.xbk.knowledge.domain.model.entity.ConfigAudit;
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

    @Override
    public PageResult<ConfigAudit> queryAuditPage(
            String tableName,
            Long recordId,
            String operator,
            int offset,
            int pageSize,
            String sortField,
            String sortOrder
    ) {
        // 解析排序字段和排序方向（白名单方式避免 SQL 注入）
        String sortColumn = resolveSortColumn(sortField);
        String resolvedSortOrder = resolveSortOrder(sortOrder);

        // 规范化查询条件
        String normalizedTableName = normalizeText(tableName);
        String normalizedOperator = normalizeText(operator);

        // 查询分页数据
        List<ConfigAudit> audits = configAuditRepository.findByConditions(
                normalizedTableName,
                recordId,
                normalizedOperator,
                offset,
                pageSize,
                sortColumn,
                resolvedSortOrder
        );

        // 查询总数
        long total = configAuditRepository.countByConditions(
                normalizedTableName,
                recordId,
                normalizedOperator
        );

        // 计算页码
        int pageNum = (offset / pageSize) + 1;

        return PageResult.of(audits, total, pageNum, pageSize);
    }

    /**
     * 规范化文本
     */
    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 解析排序字段
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
     */
    private String resolveSortOrder(String sortOrder) {
        if (!StringUtils.hasText(sortOrder)) {
            return "DESC";
        }
        String normalized = sortOrder.trim().toUpperCase();
        return (normalized.equals("ASC") || normalized.equals("DESC")) ? normalized : "DESC";
    }
}
