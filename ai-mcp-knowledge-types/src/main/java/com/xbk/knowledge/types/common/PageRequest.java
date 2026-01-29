package com.xbk.knowledge.types.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 分页请求参数类
 * 用于接收分页查询的请求参数
 * 继承 BaseRequest，包含通用请求字段
 *
 * 职责：通用基础结构，用于统一分页与响应结构
 * @author xiexu
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码（从 1 开始）
     */
    @Builder.Default
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    @Builder.Default
    private Integer pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序方向（ASC/DESC）
     */
    @Builder.Default
    private String sortOrder = "ASC";

    /**
     * 验证并修正分页参数
     * 确保页码和每页大小在合理范围内
     */
    public void validate() {
        // 页码最小为 1
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }

        // 每页大小范围：1-100
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        } else if (pageSize > 100) {
            pageSize = 100;
        }

        // 排序方向只能是 ASC 或 DESC
        if (sortOrder != null && !sortOrder.equalsIgnoreCase("ASC") && !sortOrder.equalsIgnoreCase("DESC")) {
            sortOrder = "ASC";
        }
    }

    /**
     * 计算偏移量（用于 SQL LIMIT OFFSET）
     *
     * @return 偏移量
     */
    public int getOffset() {
        return (pageNum - 1) * pageSize;
    }
}
