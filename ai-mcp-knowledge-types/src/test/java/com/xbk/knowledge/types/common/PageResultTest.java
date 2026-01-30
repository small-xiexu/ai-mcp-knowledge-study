package com.xbk.knowledge.types.common;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证分页结果的页数与分页边界计算，避免分页元数据错误。
 *
 * @author xiexu
 */
public class PageResultTest {

    /**
     * 对外暴露 shouldCalculateTotalPagesAndFlags 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldCalculateTotalPagesAndFlags() {
        List<String> records = Arrays.asList("a", "b");

        PageResult<String> result = PageResult.of(records, 21L, 1, 10);

        assertEquals(3, result.getTotalPages());
        assertTrue(result.getHasNext());
        assertFalse(result.getHasPrevious());
    }

    /**
     * 对外暴露 shouldBuildEmptyResult 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldBuildEmptyResult() {
        PageResult<String> result = PageResult.empty(2, 10);

        assertEquals(0L, result.getTotal());
        assertEquals(0, result.getTotalPages());
        assertFalse(result.getHasNext());
        assertFalse(result.getHasPrevious());
        assertEquals(2, result.getPageNum());
        assertEquals(10, result.getPageSize());
        assertTrue(result.getRecords().isEmpty());
    }
}
