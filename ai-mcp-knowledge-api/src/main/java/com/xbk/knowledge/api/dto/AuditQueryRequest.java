package com.xbk.knowledge.api.dto;

import com.xbk.knowledge.types.common.PageRequest;

import java.io.Serializable;

/**
 * 审计查询请求 DTO
 * 用于统一承载审计筛选与分页条件，避免控制器直接散落分页参数
 *
 * @author xiexu
 */
public record AuditQueryRequest(
        String tableName,
        Long recordId,
        String operator,
        Integer pageNum,
        Integer pageSize,
        String sortField,
        String sortOrder
) implements Serializable {

    /**
     * 转换为分页参数
     * 统一默认排序，确保审计日志读取顺序稳定
     *
     * @return 分页参数
     */
    public PageRequest toPageRequest() {
        var resolvedSortOrder = sortOrder;
        if (resolvedSortOrder == null || resolvedSortOrder.isBlank()) {
            resolvedSortOrder = "DESC";
        }
        return PageRequest.builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .sortField(sortField)
                .sortOrder(resolvedSortOrder)
                .build();
    }
}
