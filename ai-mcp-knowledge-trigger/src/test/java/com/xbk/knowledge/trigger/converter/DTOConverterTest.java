package com.xbk.knowledge.trigger.converter;

import com.xbk.knowledge.api.dto.ai.AIRequest;
import com.xbk.knowledge.api.dto.ai.AIResponse;
import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.AICallResult;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
                .systemPrompt("sys")
                .parameters(Collections.<String, Object>singletonMap("temperature", 0.5))
                .streaming(true)
                .build();

        AICallCommand command = DTOConverter.toAppAICallCommand(request);

        assertEquals("hi", command.getContent());
        assertEquals("sys", command.getSystemPrompt());
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
                .responseTime(5L)
                .success(true)
                .fallback(false)
                .retryCount(1)
                .build();

        AIResponse response = DTOConverter.toApiAIResponse(result);

        assertEquals("ok", response.getContent());
        assertEquals("m1", response.getModelUsed());
        assertEquals(Long.valueOf(5L), response.getResponseTime());
        assertEquals(Boolean.TRUE, response.getSuccess());
    }

}
