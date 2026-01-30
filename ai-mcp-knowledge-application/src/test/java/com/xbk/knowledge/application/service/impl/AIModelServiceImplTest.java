package com.xbk.knowledge.application.service.impl;

import com.xbk.knowledge.application.fallback.FallbackHandler;
import com.xbk.knowledge.application.fallback.ModelCallContext;
import com.xbk.knowledge.application.fallback.ModelCallExecutor;
import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.application.model.dto.ModelSelectionDecision;
import com.xbk.knowledge.application.model.dto.ModelSelectionResult;
import com.xbk.knowledge.application.service.ModelSelector;
import com.xbk.knowledge.application.service.selection.ModelSelectionChain;
import com.xbk.knowledge.domain.model.aggregate.call.CallLogAggregate;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.repository.CallLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 AI 调用编排路径中的选择与日志写入，避免调用结果遗漏。
 *
 * @author xiexu
 */
public class AIModelServiceImplTest {

    private ModelSelector modelSelector;
    private ModelSelectionChain modelSelectionChain;
    private ModelCallExecutor modelCallExecutor;
    private CallLogRepository callLogRepository;
    private FallbackHandler fallbackHandler;
    private AIModelServiceImpl service;

    /**
     * 对外暴露 setUp 作为调用入口，便于上层复用。
     */
    @BeforeEach
    public void setUp() {
        modelSelector = Mockito.mock(ModelSelector.class);
        modelSelectionChain = Mockito.mock(ModelSelectionChain.class);
        modelCallExecutor = Mockito.mock(ModelCallExecutor.class);
        callLogRepository = Mockito.mock(CallLogRepository.class);
        fallbackHandler = Mockito.mock(FallbackHandler.class);
        service = new AIModelServiceImpl(modelSelector, modelSelectionChain, modelCallExecutor, callLogRepository, fallbackHandler);
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

    /**
     * 对外暴露 shouldExecuteChatByTaskTypeWithFallbackHandler 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldExecuteChatByTaskTypeWithFallbackHandler() {
        ModelConfig primary = ModelConfig.builder().id(1L).modelName("primary").build();
        ModelSelectionResult selectionResult = ModelSelectionResult.builder()
                .primaryModel(primary)
                .fallbackModels(Collections.<ModelConfig>emptyList())
                .build();
        when(modelSelector.selectModel("task")).thenReturn(selectionResult);

        AICallResult result = AICallResult.builder()
                .success(true)
                .content("ok")
                .modelUsed("primary")
                .tokensUsed(1)
                .responseTime(1L)
                .fallback(false)
                .build();
        when(fallbackHandler.executeWithFallback(any(ModelConfig.class), anyList(), any(AICallCommand.class)))
                .thenReturn(result);

        AICallCommand command = AICallCommand.builder()
                .content("hi")
                .taskType("task")
                .build();
        AICallResult response = service.chatByTaskType(command);

        assertEquals("primary", response.getModelUsed());
        verify(callLogRepository).save(any(CallLogAggregate.class));
    }
}
