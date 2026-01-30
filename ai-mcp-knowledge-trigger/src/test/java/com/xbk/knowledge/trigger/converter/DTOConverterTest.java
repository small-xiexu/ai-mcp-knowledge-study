package com.xbk.knowledge.trigger.converter;

import com.xbk.knowledge.api.dto.ai.AIRequest;
import com.xbk.knowledge.api.dto.ai.AIResponse;
import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.domain.model.entity.ModelCapability;
import com.xbk.knowledge.api.dto.model.ModelCapabilityDTO;
import com.xbk.knowledge.types.enums.ModelSelectionStrategy;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 验证 DTO 转换器字段映射，避免接口与应用层语义偏差。
 *
 * @author xiexu
 */
public class DTOConverterTest {

    /**
     * 对外暴露 shouldConvertApiRequestToCommand 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldConvertApiRequestToCommand() {
        AIRequest request = AIRequest.builder()
                .content("hi")
                .taskType("task")
                .systemPrompt("sys")
                .parameters(Collections.<String, Object>singletonMap("temperature", 0.5))
                .strategy(ModelSelectionStrategy.QUALITY_PRIORITY)
                .streaming(true)
                .build();

        AICallCommand command = DTOConverter.toAppAICallCommand(request);

        assertEquals("hi", command.getContent());
        assertEquals("task", command.getTaskType());
        assertEquals("sys", command.getSystemPrompt());
        assertEquals(ModelSelectionStrategy.QUALITY_PRIORITY, command.getStrategy());
        assertEquals(Boolean.TRUE, command.getStreaming());
    }

    /**
     * 对外暴露 shouldConvertAppResultToApiResponse 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldConvertAppResultToApiResponse() {
        AICallResult result = AICallResult.builder()
                .content("ok")
                .modelUsed("m1")
                .tokensUsed(10)
                .responseTime(5L)
                .success(true)
                .fallback(false)
                .retryCount(1)
                .build();

        AIResponse response = DTOConverter.toApiAIResponse(result);

        assertEquals("ok", response.getContent());
        assertEquals("m1", response.getModelUsed());
        assertEquals(Integer.valueOf(10), response.getTokensUsed());
        assertEquals(Long.valueOf(5L), response.getResponseTime());
        assertEquals(Boolean.TRUE, response.getSuccess());
    }

    /**
     * 对外暴露 shouldConvertModelCapability 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldConvertModelCapability() {
        ModelCapability capability = ModelCapability.builder()
                .qualityScore(80)
                .maxInputTokens(100)
                .maxOutputTokens(200)
                .supportFunctionCalling(true)
                .supportStreaming(true)
                .supportVision(false)
                .build();

        ModelCapabilityDTO dto = DTOConverter.toApiModelCapability(capability);

        assertNotNull(dto);
        assertEquals(Integer.valueOf(80), dto.getQualityScore());
        assertEquals(Integer.valueOf(100), dto.getMaxInputTokens());
        assertEquals(Integer.valueOf(200), dto.getMaxOutputTokens());
        assertEquals(Boolean.TRUE, dto.getSupportFunctionCalling());
    }
}
