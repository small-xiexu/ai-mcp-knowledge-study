package com.xbk.knowledge.infrastructure.common;

import org.springframework.beans.BeanUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Bean 映射工具。
 *
 * @author sxie
 */
public final class BeanMappingUtils {

    /**
     * 创建对象映射工具并注入依赖组件。
     */
    private BeanMappingUtils() {
    }

    /**
     * 单对象复制。
     *
     * @param source    源对象
     * @param targetCls 目标类型
     * @param <S>       源类型
     * @param <T>       目标类型
     * @return 目标对象
     */
    public static <S, T> T map(S source, Class<T> targetCls) {
        if (source == null || targetCls == null) {
            return null;
        }
        try {
            T target = targetCls.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new IllegalStateException("Bean 映射失败: " + targetCls.getName(), e);
        }
    }

    /**
     * 列表复制。
     *
     * @param sourceList 源列表
     * @param targetCls  目标类型
     * @param <S>        源类型
     * @param <T>        目标类型
     * @return 目标列表
     */
    public static <S, T> List<T> mapList(List<S> sourceList, Class<T> targetCls) {
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyList();
        }
        return sourceList.stream()
                .filter(Objects::nonNull)
                .map(item -> map(item, targetCls))
                .collect(Collectors.toList());
    }
}

