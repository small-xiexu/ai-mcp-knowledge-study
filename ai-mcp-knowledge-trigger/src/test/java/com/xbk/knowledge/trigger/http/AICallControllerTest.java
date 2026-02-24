package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.application.service.app.AiChatAppService;
import com.xbk.knowledge.application.service.app.ModelConfigAppService;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.types.enums.ModelType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 验证 AI Controller 的请求编排与错误兜底响应，避免接口返回不一致。
 *
 * @author xiexu
 */
public class AICallControllerTest {

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

}
