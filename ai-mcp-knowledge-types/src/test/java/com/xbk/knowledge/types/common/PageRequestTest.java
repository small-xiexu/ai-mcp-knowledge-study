package com.xbk.knowledge.types.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证分页请求的参数归一化与偏移量计算，防止分页越界。
 *
 * @author xiexu
 */
public class PageRequestTest {

    /**
     * 对外暴露 shouldNormalizeInvalidValues 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldNormalizeInvalidValues() {
        PageRequest request = PageRequest.builder()
                .pageNum(0)
                .pageSize(200)
                .sortOrder("invalid")
                .build();

        request.validate();

        assertEquals(1, request.getPageNum());
        assertEquals(100, request.getPageSize());
        assertEquals("ASC", request.getSortOrder());
    }

    /**
     * 对外暴露 shouldCalculateOffset 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldCalculateOffset() {
        PageRequest request = PageRequest.builder()
                .pageNum(3)
                .pageSize(20)
                .build();

        assertEquals(40, request.getOffset());
    }
}
