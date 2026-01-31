package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.dto.metrics.ModelUsageQueryRequest;
import com.xbk.knowledge.application.service.app.MetricsAppService;
import com.xbk.knowledge.domain.model.vo.metrics.ModelUsage;
import com.xbk.knowledge.domain.model.vo.metrics.ModelUsageQuery;
import com.xbk.knowledge.types.common.Result;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证指标 Controller 的占比计算，避免统计口径错误。
 *
 * @author xiexu
 */
public class MetricsControllerTest {

    /**
     * 对外暴露 shouldCalculateUsageRate 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldCalculateUsageRate() {
        MetricsAppService appService = Mockito.mock(MetricsAppService.class);
        MetricsController controller = new MetricsController(appService);

        ModelUsage usage1 = new ModelUsage(1L, null, 1L, null);
        ModelUsage usage2 = new ModelUsage(2L, null, 3L, null);
        when(appService.collectModelUsage(any(ModelUsageQuery.class))).thenReturn(Arrays.asList(usage1, usage2));

        ModelUsageQueryRequest request = new ModelUsageQueryRequest();
        request.setStartTime(LocalDateTime.now().minusDays(1));
        request.setEndTime(LocalDateTime.now());

        Result<List<com.xbk.knowledge.api.dto.metrics.ModelUsageDTO>> result = controller.getModelUsage(request);

        assertEquals(25.0, result.getData().get(0).getUsageRate());
        assertEquals(75.0, result.getData().get(1).getUsageRate());

        ArgumentCaptor<ModelUsageQuery> captor = ArgumentCaptor.forClass(ModelUsageQuery.class);
        verify(appService).collectModelUsage(captor.capture());
        assertEquals(request.getStartTime(), captor.getValue().getStartTime());
    }
}
