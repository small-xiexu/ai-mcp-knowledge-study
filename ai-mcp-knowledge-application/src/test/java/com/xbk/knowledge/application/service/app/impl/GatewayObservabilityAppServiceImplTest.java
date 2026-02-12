package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.GatewayObservabilityAppService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class GatewayObservabilityAppServiceImplTest {

    @Test
    void shouldAggregateToolMetrics() {
        GatewayObservabilityAppService service = new GatewayObservabilityAppServiceImpl();
        service.recordCall(new GatewayObservabilityAppService.CallRecord("gw-a", "tool-x", true, null, 120, 1000));
        service.recordCall(new GatewayObservabilityAppService.CallRecord("gw-a", "tool-x", true, null, 200, 1000));
        service.recordCall(new GatewayObservabilityAppService.CallRecord("gw-a", "tool-x", false, "TOOL_EXEC_FAILED", 300, 1000));

        GatewayObservabilityAppService.GatewayMetricsReport report = service.queryMetrics(
                new GatewayObservabilityAppService.MetricsQuery("gw-a", "tool-x", 60)
        );

        Assertions.assertEquals(1, report.toolMetrics().size());
        GatewayObservabilityAppService.ToolMetricsSnapshot snapshot = report.toolMetrics().get(0);
        Assertions.assertEquals(3L, snapshot.requestCount());
        Assertions.assertEquals(66.67, snapshot.successRate());
        Assertions.assertEquals(300L, snapshot.p95LatencyMs());
        Assertions.assertEquals(300L, snapshot.p99LatencyMs());
        Assertions.assertTrue(snapshot.errorDistribution().containsKey("TOOL_EXEC_FAILED"));
    }

    @Test
    void shouldGenerateConsecutiveFailureAlert() {
        GatewayObservabilityAppService service = new GatewayObservabilityAppServiceImpl();
        service.recordCall(new GatewayObservabilityAppService.CallRecord("gw-b", "tool-y", false, "TOOL_EXEC_FAILED", 100, 500));
        service.recordCall(new GatewayObservabilityAppService.CallRecord("gw-b", "tool-y", false, "TOOL_EXEC_FAILED", 120, 500));
        service.recordCall(new GatewayObservabilityAppService.CallRecord("gw-b", "tool-y", false, "TOOL_EXEC_FAILED", 140, 500));

        GatewayObservabilityAppService.GatewayMetricsReport report = service.queryMetrics(
                new GatewayObservabilityAppService.MetricsQuery("gw-b", "tool-y", 60)
        );

        List<GatewayObservabilityAppService.AlertSnapshot> alerts = report.alerts();
        boolean hit = alerts.stream().anyMatch(alert -> "CONSECUTIVE_FAILURE".equals(alert.alertType()));
        Assertions.assertTrue(hit);
    }
}
