package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.domain.model.vo.metrics.MetricsQuery;
import com.xbk.knowledge.domain.service.metrics.IMetricsDomainService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;

/**
 * 验证监控指标应用服务的委托行为，避免指标链路遗漏。
 *
 * @author xiexu
 */
public class MetricsAppServiceImplTest {

    /**
     * 对外暴露 shouldDelegateMetricsQuery 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldDelegateMetricsQuery() {
        IMetricsDomainService domainService = Mockito.mock(IMetricsDomainService.class);
        MetricsAppServiceImpl appService = new MetricsAppServiceImpl(domainService);

        MetricsQuery query = new MetricsQuery(1L, "task", null, null);
        appService.collectCallMetrics(query);

        verify(domainService).collectCallMetrics(query);
    }
}
