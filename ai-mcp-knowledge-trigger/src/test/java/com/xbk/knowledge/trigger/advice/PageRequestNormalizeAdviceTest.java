package com.xbk.knowledge.trigger.advice;

import com.xbk.knowledge.types.common.PageRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证分页归一化 Advice 能修正非法分页参数，避免分页异常。
 *
 * @author xiexu
 */
public class PageRequestNormalizeAdviceTest {

    /**
     * 对外暴露 shouldNormalizePageRequest 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldNormalizePageRequest() {
        PageRequestNormalizeAdvice advice = new PageRequestNormalizeAdvice();
        PageRequest request = PageRequest.builder()
                .pageNum(0)
                .pageSize(200)
                .sortOrder("bad")
                .build();

        advice.afterBodyRead(request, null, null, null, null);

        assertEquals(1, request.getPageNum());
        assertEquals(100, request.getPageSize());
        assertEquals("ASC", request.getSortOrder());
    }
}
