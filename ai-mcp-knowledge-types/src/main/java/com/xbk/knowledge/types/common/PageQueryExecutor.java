package com.xbk.knowledge.types.common;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 分页查询执行器
 * 用于统一 Controller 中“组装分页查询 + 查询 + 转换 + 包装返回”的固定流程
 *
 * 职责：通用工具，用于减少分页接口样板代码
 *
 * @author sxie
 */
public final class PageQueryExecutor {

    /**
     * 常规分页默认每页条数。
     */
    private static final int DEFAULT_PAGE_SIZE = 10;

    private PageQueryExecutor() {
    }

    /**
     * 执行分页查询并转换为统一返回结构
     *
     * @param <Q>           分页查询对象类型
     * @param <S>           分页源记录类型
     * @param <R>           分页目标记录类型
     * @param request       分页请求
     * @param queryBuilder  分页查询对象构建函数（offset, pageSize -> query）
     * @param queryExecutor 分页查询执行函数（query -> PageResult）
     * @param converter     记录转换函数
     * @return 统一分页返回结果
     */
    public static <Q, S, R> Result<PageResult<R>> execute(PageRequest request,
                                                          BiFunction<Integer, Integer, Q> queryBuilder,
                                                          Function<Q, PageResult<S>> queryExecutor,
                                                          Function<S, R> converter) {
        // 核心函数必须存在，否则无法完成分页编排
        Objects.requireNonNull(queryBuilder, "queryBuilder must not be null");
        Objects.requireNonNull(queryExecutor, "queryExecutor must not be null");
        Objects.requireNonNull(converter, "converter must not be null");

        // 请求体允许为空；为空时使用默认分页参数（pageNum=1, pageSize=10）
        PageRequest safeRequest = request;
        if (safeRequest == null) {
            safeRequest = PageRequest.builder().build();
        }
        // 统一修正分页边界，避免各接口重复写页码/条数校验
        safeRequest.validate();

        // 1、用标准分页参数构建领域查询对象
        Q query = queryBuilder.apply(safeRequest.getOffset(), safeRequest.getPageSize());
        // 2、执行业务分页查询（返回 PageResult<S>）
        PageResult<S> pageResult = queryExecutor.apply(query);
        // 3、统一转换记录类型（S -> R），保留总数与分页元数据
        PageResult<R> converted = PageResultConverter.convert(pageResult, converter);
        // 4、统一包装 Result.success 返回给 Controller
        return Result.success(converted);
    }

    /**
     * 执行 `PageRequest` 风格的分页查询并转换为统一返回结构。
     * <p>
     * 适用于应用服务直接接收 `(offset, pageSize)` 的场景。
     *
     * @param <S>           分页源记录类型
     * @param <R>           分页目标记录类型
     * @param request       分页请求
     * @param queryExecutor 分页查询执行函数（offset, pageSize -> PageResult）
     * @param converter     记录转换函数
     * @return 统一分页返回结果
     */
    public static <S, R> Result<PageResult<R>> execute(PageRequest request,
                                                        BiFunction<Integer, Integer, PageResult<S>> queryExecutor,
                                                        Function<S, R> converter) {
        Objects.requireNonNull(queryExecutor, "queryExecutor must not be null");
        Objects.requireNonNull(converter, "converter must not be null");

        PageRequest safeRequest = request;
        if (safeRequest == null) {
            safeRequest = PageRequest.builder().build();
        }
        safeRequest.validate();

        // 1、按归一化后的 offset/pageSize 执行业务分页
        PageResult<S> pageResult = queryExecutor.apply(safeRequest.getOffset(), safeRequest.getPageSize());
        // 2、统一转换记录类型（S -> R）
        PageResult<R> converted = PageResultConverter.convert(pageResult, converter);
        // 3、统一包装 Result.success 返回
        return Result.success(converted);
    }

    /**
     * 执行 offset/pageSize 风格的分页查询并转换为统一返回结构。
     * <p>
     * 适用于请求对象未继承 `PageRequest`，但包含 `offset/pageSize` 字段的场景。
     *
     * @param <S>              分页源记录类型
     * @param <R>              分页目标记录类型
     * @param offset           偏移量
     * @param pageSize         每页条数
     * @param defaultPageSize  pageSize 为空或非法时的默认值
     * @param queryExecutor    分页查询执行函数（offset, pageSize -> PageResult）
     * @param converter        记录转换函数
     * @return 统一分页返回结果
     */
    public static <S, R> Result<PageResult<R>> execute(Integer offset,
                                                       Integer pageSize,
                                                       BiFunction<Integer, Integer, PageResult<S>> queryExecutor,
                                                       Function<S, R> converter) {
        return execute(offset, pageSize, DEFAULT_PAGE_SIZE, queryExecutor, converter);
    }

    /**
     * 执行 offset/pageSize 风格的分页查询并转换为统一返回结构。
     * <p>
     * 适用于请求对象未继承 `PageRequest`，但包含 `offset/pageSize` 字段的场景。
     *
     * @param <S>              分页源记录类型
     * @param <R>              分页目标记录类型
     * @param offset           偏移量
     * @param pageSize         每页条数
     * @param defaultPageSize  pageSize 为空或非法时的默认值
     * @param queryExecutor    分页查询执行函数（offset, pageSize -> PageResult）
     * @param converter        记录转换函数
     * @return 统一分页返回结果
     */
    public static <S, R> Result<PageResult<R>> execute(Integer offset,
                                                       Integer pageSize,
                                                       int defaultPageSize,
                                                       BiFunction<Integer, Integer, PageResult<S>> queryExecutor,
                                                       Function<S, R> converter) {
        Objects.requireNonNull(queryExecutor, "queryExecutor must not be null");
        Objects.requireNonNull(converter, "converter must not be null");

        int safeOffset = PageParamUtils.normalizeOffset(offset);
        int safePageSize = PageParamUtils.normalizePageSize(pageSize, defaultPageSize);

        // 1、按归一化后的 offset/pageSize 执行业务分页
        PageResult<S> pageResult = queryExecutor.apply(safeOffset, safePageSize);
        // 2、统一转换记录类型（S -> R）
        PageResult<R> converted = PageResultConverter.convert(pageResult, converter);
        // 3、统一包装 Result.success 返回
        return Result.success(converted);
    }

    /**
     * 执行 pageNum/pageSize 风格的分页查询并转换为统一返回结构。
     * <p>
     * 适用于应用服务方法签名使用 `(pageNum, pageSize)` 的场景。
     *
     * @param <S>              分页源记录类型
     * @param <R>              分页目标记录类型
     * @param pageNum          页码（从1开始）
     * @param pageSize         每页条数
     * @param queryExecutor    分页查询执行函数（pageNum, pageSize -> PageResult）
     * @param converter        记录转换函数
     * @return 统一分页返回结果
     */
    public static <S, R> Result<PageResult<R>> executeByPageNum(Integer pageNum,
                                                                Integer pageSize,
                                                                BiFunction<Integer, Integer, PageResult<S>> queryExecutor,
                                                                Function<S, R> converter) {
        return executeByPageNum(pageNum, pageSize, DEFAULT_PAGE_SIZE, queryExecutor, converter);
    }

    /**
     * 执行 pageNum/pageSize 风格的分页查询并转换为统一返回结构。
     * <p>
     * 适用于应用服务方法签名使用 `(pageNum, pageSize)` 的场景。
     *
     * @param <S>              分页源记录类型
     * @param <R>              分页目标记录类型
     * @param pageNum          页码（从1开始）
     * @param pageSize         每页条数
     * @param defaultPageSize  pageSize 为空或非法时的默认值
     * @param queryExecutor    分页查询执行函数（pageNum, pageSize -> PageResult）
     * @param converter        记录转换函数
     * @return 统一分页返回结果
     */
    public static <S, R> Result<PageResult<R>> executeByPageNum(Integer pageNum,
                                                                Integer pageSize,
                                                                int defaultPageSize,
                                                                BiFunction<Integer, Integer, PageResult<S>> queryExecutor,
                                                                Function<S, R> converter) {
        Objects.requireNonNull(queryExecutor, "queryExecutor must not be null");
        Objects.requireNonNull(converter, "converter must not be null");

        int safePageNum = PageParamUtils.normalizePageNum(pageNum);
        int safePageSize = PageParamUtils.normalizePageSize(pageSize, defaultPageSize);

        // 1、按归一化后的 pageNum/pageSize 执行业务分页
        PageResult<S> pageResult = queryExecutor.apply(safePageNum, safePageSize);
        // 2、统一转换记录类型（S -> R）
        PageResult<R> converted = PageResultConverter.convert(pageResult, converter);
        // 3、统一包装 Result.success 返回
        return Result.success(converted);
    }
}
