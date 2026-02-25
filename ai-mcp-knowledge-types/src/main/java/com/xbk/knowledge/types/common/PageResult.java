package com.xbk.knowledge.types.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应结果类
 * 用于封装分页查询的响应数据
 *
 * 职责：通用基础结构，用于统一分页与响应结构
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 数据列表
     */
    private List<T> records;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 总页数
     */
    private Integer totalPages;

    /**
     * 是否有下一页
     */
    private Boolean hasNext;

    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;

    /**
     * 创建分页结果
     * 
     * @param records 数据列表
     * @param total 总记录数
     * @param pageNum 当前页码
     * @param pageSize 每页大小
     * @return PageResult
     */
    public static <T> PageResult<T> of(List<T> records, Long total, Integer pageNum, Integer pageSize) {
        // 计算总页数
        int totalPages = (int) Math.ceil((double) total / pageSize);
        boolean hasNext = pageNum < totalPages;
        boolean hasPrevious = pageNum > 1;

        return PageResult.<T>builder()
                .records(records)
                .total(total)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .build();
    }

    /**
     * 创建空的分页结果
     * 
     * @param pageNum 当前页码
     * @param pageSize 每页大小
     * @return PageResult
     */
    public static <T> PageResult<T> empty(Integer pageNum, Integer pageSize) {
        List<T> emptyRecords = List.of();
        return PageResult.<T>builder()
                .records(emptyRecords)
                .total(0L)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages(0)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }
}
