package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.dto.model.ModelCapabilityRequest;
import com.xbk.knowledge.api.dto.model.ModelConfigQueryRequest;
import com.xbk.knowledge.api.dto.model.ModelConfigRequest;
import com.xbk.knowledge.api.dto.model.ModelConfigResponse;
import com.xbk.knowledge.application.service.app.ModelConfigAppService;
import com.xbk.knowledge.domain.model.entity.ModelCapability;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.vo.model.ModelConfigPageQuery;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.types.enums.ModelType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证模型配置 Controller 的请求转换与返回装配，避免能力字段遗漏。
 *
 * @author xiexu
 */
public class ModelConfigControllerTest {

    /**
     * 对外暴露 shouldCreateModelWithCapability 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldCreateModelWithCapability() {
        ModelConfigAppService appService = Mockito.mock(ModelConfigAppService.class);
        ModelConfigController controller = new ModelConfigController(appService);

        ModelCapability capability = ModelCapability.builder().maxInputTokens(10).qualityScore(80).build();
        ModelConfig saved = ModelConfig.builder()
                .id(1L)
                .modelName("m1")
                .modelType(ModelType.OPENAI)
                .capability(capability)
                .build();
        when(appService.createModelConfig(any(ModelConfig.class))).thenReturn(saved);

        ModelCapabilityRequest capabilityRequest = ModelCapabilityRequest.builder()
                .maxTokens(10)
                .qualityScore(80)
                .build();
        ModelConfigRequest request = ModelConfigRequest.builder()
                .modelName("m1")
                .modelType(ModelType.OPENAI)
                .apiKey("k")
                .baseUrl("url")
                .capability(capabilityRequest)
                .build();

        Result<ModelConfigResponse> result = controller.createModel(request);

        assertEquals("m1", result.getData().getModelName());
        assertEquals(Integer.valueOf(10), result.getData().getCapability().getMaxInputTokens());
        assertEquals(Integer.valueOf(80), result.getData().getCapability().getQualityScore());
    }

    /**
     * 对外暴露 shouldListModels 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldListModels() {
        ModelConfigAppService appService = Mockito.mock(ModelConfigAppService.class);
        ModelConfigController controller = new ModelConfigController(appService);

        ModelConfig model = ModelConfig.builder().id(1L).modelName("m1").modelType(ModelType.OPENAI).build();
        PageResult<ModelConfig> pageResult = PageResult.of(Collections.<ModelConfig>singletonList(model), 1L, 1, 10);
        when(appService.queryModelConfigPage(any(ModelConfigPageQuery.class))).thenReturn(pageResult);

        ModelConfigQueryRequest request = new ModelConfigQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        request.validate();

        Result<PageResult<ModelConfigResponse>> result = controller.listModels(request);

        assertEquals(1, result.getData().getRecords().size());
        assertEquals("m1", result.getData().getRecords().get(0).getModelName());

        ArgumentCaptor<ModelConfigPageQuery> captor = ArgumentCaptor.forClass(ModelConfigPageQuery.class);
        verify(appService).queryModelConfigPage(captor.capture());
        assertEquals(0, captor.getValue().getOffset());
        assertEquals(10, captor.getValue().getPageSize());
    }
}
