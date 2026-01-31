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
 * @author xiexu
 */
public final class PageResultConverter {

    private PageResultConverter() {
    }

    /**
     * 将分页结果中的 records 转换为目标类型
     *
     * @param source    原始分页结果
     * @param converter 记录转换函数
     * @param <T>       原始记录类型
     * @param <R>       目标记录类型
     * @return 转换后的分页结果
     */
    public static <T, R> PageResult<R> convert(PageResult<T> source, Function<T, R> converter) {
        /**
         * 允许 source 为空时返回默认分页结构，
         * 这样可以避免调用侧空指针并保持分页响应结构稳定。
         */
        if (source == null) {
            return PageResult.of(Collections.emptyList(), 0L, 1, 10);
        }
        /**
         * 先取出原始记录列表再做转换，
         * 避免重复访问 source 并便于做空列表的快速处理。
         */
        List<T> records = source.getRecords();
        List<R> targetRecords;
        /**
         * 记录为空时直接返回空列表，
         * 减少不必要的 stream 开销并保持响应语义一致。
         */
        if (records == null || records.isEmpty()) {
            targetRecords = Collections.emptyList();
        } else {
            /**
             * 通过 converter 将每条记录映射为目标类型，
             * 将转换逻辑集中在这里以统一分页转换的行为。
             */
            targetRecords = records
                    .stream()
                    .map(converter)
                    .collect(Collectors.toList());
        }

        /**
         * 复用原始分页元信息（total/pageNum/pageSize），
         * 只替换 records 列表，避免分页信息在转换过程中丢失。
         */
        return PageResult.of(
                targetRecords,
                source.getTotal(),
                source.getPageNum(),
                source.getPageSize()
        );
    }
}
