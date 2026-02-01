package com.xbk.knowledge.domain.model.vo.audit;

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
     *
     * 为什么：按表名筛选审计记录
     */
    private String tableName;

    /**
     * 偏移量
     *
     * 为什么：用于分页计算起始位置
     */
    private Integer offset;

    /**
     * 每页大小
     *
     * 为什么：控制单次返回数量
     */
    private Integer pageSize;

    /**
     * 排序字段
     *
     * 为什么：支持可控排序
     */
    private String sortField;

    /**
     * 排序方向
     *
     * 为什么：支持升降序控制
     */
    private String sortOrder;
}
