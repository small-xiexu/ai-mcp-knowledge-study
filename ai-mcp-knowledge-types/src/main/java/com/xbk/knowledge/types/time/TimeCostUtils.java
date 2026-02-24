package com.xbk.knowledge.types.time;

import java.util.concurrent.TimeUnit;

/**
 * 耗时计算工具类
 * 统一使用单调时间源，避免系统时间回拨导致耗时异常
 *
 * @author sxie
 */
public final class TimeCostUtils {

    /**
     * 创建耗时工具并注入依赖组件。
     */
    private TimeCostUtils() {
    }

    /**
     * 开始计时
     *
     * @return 计时起点（纳秒）
     */
    public static long start() {
        return System.nanoTime();
    }

    /**
     * 计算耗时（毫秒）
     *
     * @param startNano 计时起点（纳秒）
     * @return 耗时（毫秒）
     */
    public static long costMillis(long startNano) {
        long elapsedNano = System.nanoTime() - startNano;
        return TimeUnit.NANOSECONDS.toMillis(elapsedNano);
    }
}
