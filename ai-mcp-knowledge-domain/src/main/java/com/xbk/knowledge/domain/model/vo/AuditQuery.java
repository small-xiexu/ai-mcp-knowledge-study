package com.xbk.knowledge.domain.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审计查询条件值对象
 * 统一承载审计分页与排序条件
 *
 * 职责：领域值对象，用于表达查询条件语义
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditQuery {

    /**
     * 表名（可选）
     */
    private String tableName;

    /**
     * 记录 ID（可选）
     */
    private Long recordId;

    /**
     * 操作人（可选）
     */
    private String operator;

    /**
     * 偏移量
     */
    private Integer offset;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序方向
     */
    private String sortOrder;
}
