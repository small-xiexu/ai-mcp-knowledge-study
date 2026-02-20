package com.xbk.knowledge.domain.service.audit.impl;

import com.xbk.knowledge.domain.model.entity.ConfigAudit;
import com.xbk.knowledge.domain.model.vo.audit.AuditQuery;
import com.xbk.knowledge.domain.model.adapter.repository.audit.ConfigAuditRepository;
import com.xbk.knowledge.domain.service.audit.IAuditService;
import com.xbk.knowledge.types.common.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
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
     *
     * 为什么：统一过滤与排序规则，降低 SQL 注入风险
     * 入参：审计查询对象
     * 出参：分页结果
     */
    @Override
    public PageResult<ConfigAudit> queryAuditPage(AuditQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("审计查询条件不能为空");
        }
        /*
         * 目的：通过白名单映射排序字段，避免注入风险
         */
        String sortColumn = resolveSortColumn(query.getSortField());
        String resolvedSortOrder = resolveSortOrder(query.getSortOrder());

        /*
         * 目的：规范化输入，避免空白或非法值影响查询
         */
        String normalizedTableName = normalizeText(query.getTableName());
        /*
         * 目的：构建标准化查询对象，统一仓储查询口径
         */
        AuditQuery normalizedQuery = new AuditQuery(
                normalizedTableName,
                query.getOffset(),
                query.getPageSize(),
                sortColumn,
                resolvedSortOrder
        );
        List<ConfigAudit> audits = configAuditRepository.findByConditions(normalizedQuery);

        /*
         * 目的：查询总数用于分页展示
         */
        long total = configAuditRepository.countByConditions(normalizedQuery);

        /*
         * 目的：将偏移量转换为页码以保持响应一致
         */
        Integer offset = query.getOffset();
        Integer pageSize = query.getPageSize();
        int pageNum = (offset / pageSize) + 1;

        return PageResult.of(audits, total, pageNum, pageSize);
    }

    /**
     * 查询所有可用表名
     *
     * 为什么：提供审计表筛选下拉数据源
     * 入参：无
     * 出参：表名列表
     */
    @Override
    public List<String> listTableNames() {
        List<String> tableNames = configAuditRepository.listTableNames();
        return tableNames == null ? Collections.emptyList() : tableNames;
    }

    /**
     * 规范化文本
     * 统一去空白并处理空值
     *
     * 为什么：避免空白字符串影响查询逻辑
     * 入参：文本
     * 出参：规范化文本
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
     *
     * 为什么：强制白名单字段映射，防止注入
     * 入参：排序字段
     * 出参：排序列名
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
     *
     * 为什么：限制排序方向，防止注入或非法值
     * 入参：排序方向
     * 出参：规范化排序方向
     */
    private String resolveSortOrder(String sortOrder) {
        if (!StringUtils.hasText(sortOrder)) {
            return "DESC";
        }
        String normalized = sortOrder
                .trim()
                .toUpperCase();
        return ("ASC".equals(normalized) || "DESC".equals(normalized)) ? normalized : "DESC";
    }
}
