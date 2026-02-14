package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.dto.agent.AgentRuntimeInvokeRequest;
import com.xbk.knowledge.application.service.app.AgentRuntimeAppService;
import com.xbk.knowledge.types.context.OrgContext;
import com.xbk.knowledge.types.context.OrgContextHolder;
import com.xbk.knowledge.types.contract.PlatformContractV1;
import com.xbk.knowledge.types.common.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 AgentRuntimeController 的 org 隔离规则：
 * - HTTP 入口不得信任请求体 orgId，必须以 OrgContext 为准
 *
 * @author xiexu
 */
public class AgentRuntimeControllerTest {

    @AfterEach
    public void tearDown() {
        OrgContextHolder.clear();
    }

    @Test
    public void shouldIgnoreRequestOrgIdForInvoke() {
        AgentRuntimeAppService appService = Mockito.mock(AgentRuntimeAppService.class);
        AgentRuntimeController controller = new AgentRuntimeController(appService);

        OrgContextHolder.set(new OrgContext(
                10L,
                100L,
                200L,
                false,
                false
        ));

        when(appService.invoke(any(), any(), any(), any(), any()))
                .thenReturn(PlatformContractV1.builder().status("SUCCESS").answer("ok").uncertainty("").build());

        AgentRuntimeInvokeRequest req = AgentRuntimeInvokeRequest.builder()
                .orgId(999L)
                .content("hi")
                .build();

        Result<PlatformContractV1> result = controller.invoke("a1", req);

        ArgumentCaptor<Long> orgIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(appService).invoke(orgIdCaptor.capture(), Mockito.eq("a1"), Mockito.isNull(), Mockito.eq("hi"), Mockito.isNull());
        assertEquals(200L, orgIdCaptor.getValue());
        assertEquals("SUCCESS", result.getData().getStatus());
    }

    @Test
    public void shouldFallbackToRootOrgWhenNoContext() {
        AgentRuntimeAppService appService = Mockito.mock(AgentRuntimeAppService.class);
        AgentRuntimeController controller = new AgentRuntimeController(appService);

        when(appService.invoke(any(), any(), any(), any(), any()))
                .thenReturn(PlatformContractV1.builder().status("SUCCESS").answer("ok").uncertainty("").build());

        AgentRuntimeInvokeRequest req = AgentRuntimeInvokeRequest.builder()
                .orgId(999L)
                .content("hi")
                .build();

        controller.invoke("a1", req);

        ArgumentCaptor<Long> orgIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(appService).invoke(orgIdCaptor.capture(), Mockito.eq("a1"), Mockito.isNull(), Mockito.eq("hi"), Mockito.isNull());
        assertEquals(1L, orgIdCaptor.getValue());
    }
}

