package com.xbk.knowledge.types.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证耗时工具返回非负值，避免计时异常。
 *
 * @author xiexu
 */
public class TimeCostUtilsTest {

    /**
     * 对外暴露 shouldReturnNonNegativeCost 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldReturnNonNegativeCost() throws InterruptedException {
        long start = TimeCostUtils.start();
        Thread.sleep(1L);
        long costMillis = TimeCostUtils.costMillis(start);

        assertTrue(costMillis >= 0L);
    }
}
