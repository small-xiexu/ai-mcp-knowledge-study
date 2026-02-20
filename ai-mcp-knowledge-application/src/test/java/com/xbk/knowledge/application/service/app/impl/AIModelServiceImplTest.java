package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.fallback.core.ModelCallContext;
import com.xbk.knowledge.application.fallback.executor.ModelCallExecutor;
import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.application.model.dto.ModelSelectionDecision;
import com.xbk.knowledge.application.service.selection.chain.ModelSelectionChain;
import com.xbk.knowledge.domain.model.aggregate.call.CallLogAggregate;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.adapter.repository.metrics.CallLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 AI 调用编排路径中的选择与日志写入，避免调用结果遗漏。
 *
 * @author xiexu
 */
public class AIModelServiceImplTest {

    private ModelSelectionChain modelSelectionChain;
    private ModelCallExecutor modelCallExecutor;
    private CallLogRepository callLogRepository;
    private AIModelServiceImpl service;

    /**
     * 对外暴露 setUp 作为调用入口，便于上层复用。
     */
    @BeforeEach
    public void setUp() {
        modelSelectionChain = Mockito.mock(ModelSelectionChain.class);
        modelCallExecutor = Mockito.mock(ModelCallExecutor.class);
        callLogRepository = Mockito.mock(CallLogRepository.class);
        service = new AIModelServiceImpl(modelSelectionChain, modelCallExecutor, callLogRepository);
    }

    /**
     * 对外暴露 shouldExecuteChatWithSelectedModel 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldExecuteChatWithSelectedModel() {
        ModelConfig modelConfig = ModelConfig.builder().id(1L).modelName("m1").build();
        when(modelSelectionChain.select(any(AICallCommand.class)))
                .thenReturn(ModelSelectionDecision.byModel(modelConfig));
        AICallResult result = AICallResult.builder()
                .success(true)
                .content("ok")
                .modelUsed("m1")
                .tokensUsed(10)
                .responseTime(5L)
                .build();
        when(modelCallExecutor.execute(any(ModelCallContext.class))).thenReturn(result);

        AICallResult response = service.chat(AICallCommand.builder().content("hi").build());

        assertTrue(response.getSuccess());
        assertFalse(response.getFallback());
        ArgumentCaptor<CallLogAggregate> captor =
                ArgumentCaptor.forClass(CallLogAggregate.class);
        verify(callLogRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getCallLog().getModelId());
    }

}
