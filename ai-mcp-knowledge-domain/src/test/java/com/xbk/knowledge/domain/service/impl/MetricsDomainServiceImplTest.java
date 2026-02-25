package com.xbk.knowledge.domain.service.impl;

import com.xbk.knowledge.domain.metrics.model.valobj.CallMetrics;
import com.xbk.knowledge.domain.metrics.model.valobj.MetricsQuery;
import com.xbk.knowledge.domain.metrics.model.valobj.ModelUsage;
import com.xbk.knowledge.domain.metrics.model.valobj.ModelUsageQuery;
import com.xbk.knowledge.domain.metrics.adapter.repository.CallLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证指标统计的时间范围校验与空数据归一化逻辑。
 *
 * @author xiexu
 */
public class MetricsDomainServiceImplTest {

    /**
     * 调用日志仓储。
     */
    private CallLogRepository callLogRepository;

    /**
     * 指标领域服务实现。
     */
    private MetricsDomainServiceImpl service;

    /**
     * 对外暴露 setUp 作为调用入口，便于上层复用。
     */
    @BeforeEach
    public void setUp() {
        callLogRepository = Mockito.mock(CallLogRepository.class);
        service = new MetricsDomainServiceImpl(callLogRepository);
    }

    /**
     * 对外暴露 shouldRejectInvalidTimeRange 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldRejectInvalidTimeRange() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.minusMinutes(1);
        MetricsQuery query = new MetricsQuery(1L, "task", start, end);

        assertThrows(IllegalArgumentException.class, () -> service.collectCallMetrics(query));
    }

    /**
     * 对外暴露 shouldNormalizeNullCallMetrics 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldNormalizeNullCallMetrics() {
        MetricsQuery query = new MetricsQuery(1L, "task", null, null);
        Mockito.when(callLogRepository.aggregateCallMetrics(query)).thenReturn(null);

        CallMetrics metrics = service.collectCallMetrics(query);

        assertEquals(0L, metrics.getTotalCalls());
        assertEquals(0L, metrics.getSuccessCalls());
        assertEquals(0L, metrics.getFailedCalls());
    }

    /**
     * 对外暴露 shouldReturnEmptyUsageListWhenNull 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldReturnEmptyUsageListWhenNull() {
        ModelUsageQuery query = new ModelUsageQuery(null, null);
        Mockito.when(callLogRepository.aggregateModelUsage(query)).thenReturn(null);

        List<ModelUsage> usageList = service.collectModelUsage(query);

        assertEquals(0, usageList.size());
    }
}
