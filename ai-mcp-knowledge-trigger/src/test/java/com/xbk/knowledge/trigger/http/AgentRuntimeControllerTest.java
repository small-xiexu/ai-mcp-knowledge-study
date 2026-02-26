package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.dto.agent.AgentRuntimeInvokeRequest;
import com.xbk.knowledge.application.service.app.AgentRuntimeAppService;
import com.xbk.knowledge.types.contract.PlatformContractV1;
import com.xbk.knowledge.types.common.Result;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 AgentRuntimeController 的 invoke 参数映射规则。
 *
 * @author xiexu
 */
public class AgentRuntimeControllerTest {

    @Test
    public void shouldMapInvokeRequestToAppService() {
        AgentRuntimeAppService appService = Mockito.mock(AgentRuntimeAppService.class);
        AgentRuntimeController controller = new AgentRuntimeController(appService);

        when(appService.invoke(anyString(), any(), anyString(), any()))
                .thenReturn(PlatformContractV1.builder().status("SUCCESS").answer("ok").uncertainty("").build());

        AgentRuntimeInvokeRequest req = AgentRuntimeInvokeRequest.builder()
                .content("hi")
                .build();

        Result<PlatformContractV1> result = controller.invoke("a1", req);

        verify(appService).invoke(Mockito.eq("a1"), Mockito.isNull(), Mockito.eq("hi"), Mockito.isNull());
        assertEquals("SUCCESS", result.getData().getStatus());
    }

    @Test
    public void shouldPassSessionIdAndRagTags() {
        AgentRuntimeAppService appService = Mockito.mock(AgentRuntimeAppService.class);
        AgentRuntimeController controller = new AgentRuntimeController(appService);

        when(appService.invoke(anyString(), any(), anyString(), any()))
                .thenReturn(PlatformContractV1.builder().status("SUCCESS").answer("ok").uncertainty("").build());

        AgentRuntimeInvokeRequest req = AgentRuntimeInvokeRequest.builder()
                .sessionId(9L)
                .content("hi")
                .ragTagsJson("[\"a\"]")
                .build();

        controller.invoke("a1", req);

        verify(appService).invoke(Mockito.eq("a1"), Mockito.eq(9L), Mockito.eq("hi"), Mockito.eq("[\"a\"]"));
    }
}
