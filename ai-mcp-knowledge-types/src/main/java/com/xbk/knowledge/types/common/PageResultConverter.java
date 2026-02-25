package com.xbk.knowledge.types.common;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页结果转换工具
 * 用于将 PageResult<T> 转换为 PageResult<R>
 *
 * 职责：通用工具，用于减少分页转换重复代码
 * @author sxie
 */
public final class PageResultConverter {

    /**
     * 创建分页结果转换器并注入依赖组件。
     * 
     */
    private PageResultConverter() {
    }

    /**
     * 将分页结果中的 records 转换为目标类型
     *
     * @param <T>       原始记录类型
     * @param <R>       目标记录类型
     * 
     * @param source 原始分页结果
     * @param converter 记录转换函数
     * @return 转换后的分页结果
     */
    public static <T, R> PageResult<R> convert(PageResult<T> source, Function<T, R> converter) {
        if (source == null) {
            return PageResult.of(Collections.emptyList(), 0L, 1, 10);
        }
        
        List<T> records = source.getRecords();
        List<R> targetRecords;
        
        if (records == null || records.isEmpty()) {
            targetRecords = Collections.emptyList();
        } else {
            targetRecords = records
                    .stream()
                    .map(converter)
                    .collect(Collectors.toList());
        }

        
        return PageResult.of(
                targetRecords,
                source.getTotal(),
                source.getPageNum(),
                source.getPageSize()
        );
    }
}
