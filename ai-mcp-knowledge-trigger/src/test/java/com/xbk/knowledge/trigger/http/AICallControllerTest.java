package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.dto.ai.AIRequest;
import com.xbk.knowledge.api.dto.ai.AIResponse;
import com.xbk.knowledge.api.dto.ai.ModelRecommendRequest;
import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.application.service.app.AiChatAppService;
import com.xbk.knowledge.application.service.app.ModelConfigAppService;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.vo.common.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypeQuery;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.types.common.ResultCode;
import com.xbk.knowledge.types.enums.ModelType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 验证 AI Controller 的请求编排与错误兜底响应，避免接口返回不一致。
 *
 * @author xiexu
 */
public class AICallControllerTest {

    /**
     * 对外暴露 shouldReturnSuccessForChat 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldReturnSuccessForChat() {
        AiChatAppService aiChatAppService = Mockito.mock(AiChatAppService.class);
        ModelConfigAppService modelConfigAppService = Mockito.mock(ModelConfigAppService.class);
        AICallController controller = new AICallController(modelConfigAppService, aiChatAppService);

        AICallResult callResult = AICallResult.builder()
                .success(true)
                .content("ok")
                .modelUsed("m1")
                .build();
        when(aiChatAppService.chat(any())).thenReturn(callResult);

        Result<AIResponse> result = controller.chat(AIRequest.builder().content("hi").build());

        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertEquals("ok", result.getData().getContent());
    }

    /**
     * 对外暴露 shouldReturnErrorWhenChatFails 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldReturnErrorWhenChatFails() {
        AiChatAppService aiChatAppService = Mockito.mock(AiChatAppService.class);
        ModelConfigAppService modelConfigAppService = Mockito.mock(ModelConfigAppService.class);
        AICallController controller = new AICallController(modelConfigAppService, aiChatAppService);

        when(aiChatAppService.chat(any())).thenThrow(new RuntimeException("boom"));

        Result<AIResponse> result = controller.chat(AIRequest.builder().content("hi").build());

        assertEquals(ResultCode.AI_CALL_FAILED.getCode(), result.getCode());
        assertTrue(result.getMessage().contains("boom"));
        assertEquals(Boolean.FALSE, result.getData().getSuccess());
    }

    /**
     * 对外暴露 shouldReturnAvailableModels 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldReturnAvailableModels() {
        AiChatAppService aiChatAppService = Mockito.mock(AiChatAppService.class);
        ModelConfigAppService modelConfigAppService = Mockito.mock(ModelConfigAppService.class);
        AICallController controller = new AICallController(modelConfigAppService, aiChatAppService);

        ModelConfig model = ModelConfig.builder().id(1L).modelName("m1").modelType(ModelType.OPENAI).build();
        when(modelConfigAppService.queryEnabledModels(any(EnabledQuery.class))).thenReturn(Collections.<ModelConfig>singletonList(model));

        Result<List<com.xbk.knowledge.api.dto.ai.ModelInfo>> result = controller.getAvailableModels();

        assertEquals(1, result.getData().size());
        assertEquals("m1", result.getData().get(0).getModelName());
    }

    /**
     * 对外暴露 shouldReturnErrorWhenNoRecommendation 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldReturnErrorWhenNoRecommendation() {
        AiChatAppService aiChatAppService = Mockito.mock(AiChatAppService.class);
        ModelConfigAppService modelConfigAppService = Mockito.mock(ModelConfigAppService.class);
        AICallController controller = new AICallController(modelConfigAppService, aiChatAppService);

        when(modelConfigAppService.getRecommendedModel(any(TaskTypeQuery.class))).thenReturn(null);

        Result<com.xbk.knowledge.api.dto.ai.ModelInfo> result = controller.getRecommendedModel(ModelRecommendRequest.builder().taskType("t").build());

        assertEquals(ResultCode.NOT_FOUND.getCode(), result.getCode());
    }

}
