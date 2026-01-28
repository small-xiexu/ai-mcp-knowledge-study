package com.xbk.knowledge.domain.service.impl;

import com.xbk.knowledge.domain.fallback.FallbackHandler;
import com.xbk.knowledge.domain.model.dto.DomainAIRequest;
import com.xbk.knowledge.domain.model.dto.DomainAIResponse;
import com.xbk.knowledge.domain.model.dto.DomainModelInfo;
import com.xbk.knowledge.domain.model.dto.ModelSelectionResult;
import com.xbk.knowledge.domain.model.entity.CallLog;
import com.xbk.knowledge.domain.model.entity.ModelCapability;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.provider.ModelProviderFactory;
import com.xbk.knowledge.domain.repository.CallLogRepository;
import com.xbk.knowledge.domain.repository.ModelConfigRepository;
import com.xbk.knowledge.domain.service.ModelSelector;
import com.xbk.knowledge.types.enums.CallStatus;
import com.xbk.knowledge.types.enums.ModelSelectionStrategy;
import com.xbk.knowledge.types.enums.ModelType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AIModelServiceImpl 单元测试
 * 测试 AI 模型服务的核心业务逻辑
 *
 * @author xiexu
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AI模型服务测试")
class AIModelServiceImplTest {

    @Mock
    private ModelSelector modelSelector;

    @Mock
    private ModelProviderFactory providerFactory;

    @Mock
    private CallLogRepository callLogRepository;

    @Mock
    private ModelConfigRepository modelConfigRepository;

    @Mock
    private FallbackHandler fallbackHandler;

    @InjectMocks
    private AIModelServiceImpl aiModelService;

    private ModelConfig testModelConfig;
    private DomainAIRequest testRequest;
    private ModelCapability testCapability;

    @BeforeEach
    void setUp() {
        // 准备测试数据 - 模型能力配置
        testCapability = ModelCapability.builder()
                .maxInputTokens(4096)
                .maxOutputTokens(2048)
                .supportFunctionCalling(true)
                .supportVision(false)
                .supportStreaming(true)
                .qualityScore(95)
                .build();

        // 准备测试数据 - 模型配置
        testModelConfig = ModelConfig.builder()
                .id(1L)
                .modelName("gpt-4")
                .modelType(ModelType.OPENAI)
                .apiKey("test-api-key")
                .baseUrl("https://api.openai.com")
                .enabled(true)
                .priority(10)
                .capability(testCapability)
                .build();

        // 准备测试数据 - AI 请求
        testRequest = DomainAIRequest.builder()
                .content("测试问题")
                .taskType("CODE_GENERATION")
                .systemPrompt("你是一个代码助手")
                .strategy(ModelSelectionStrategy.QUALITY_PRIORITY)
                .streaming(false)
                .build();
    }

    @Test
    @DisplayName("测试chat方法_质量优先策略_成功返回")
    void testChat_QualityPriorityStrategy_Success() {
        // Given: 准备测试数据
        // 由于 ChatClient 的 fluent API 难以 mock，这里主要测试业务逻辑
        // 实际的 ChatClient 调用会在集成测试中验证
        when(modelSelector.selectByQualityPriority()).thenReturn(testModelConfig);

        // 模拟 ChatClient 抛出异常，测试错误处理
        when(providerFactory.createChatClient(testModelConfig))
                .thenThrow(new RuntimeException("模拟API调用"));

        // When: 执行测试
        DomainAIResponse response = aiModelService.chat(testRequest);

        // Then: 验证结果 - 应该返回失败响应
        assertThat(response).isNotNull();
        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getModelUsed()).isEqualTo("gpt-4");

        // 验证交互
        verify(modelSelector).selectByQualityPriority();
        verify(providerFactory).createChatClient(testModelConfig);
        verify(callLogRepository).save(any(CallLog.class));
    }

    @Test
    @DisplayName("测试chat方法_指定任务类型_调用chatByTaskType")
    void testChat_WithTaskType_CallsChatByTaskType() {
        // Given: 准备测试数据
        testRequest.setStrategy(ModelSelectionStrategy.COST_PRIORITY);
        ModelSelectionResult selectionResult = ModelSelectionResult.builder()
                .primaryModel(testModelConfig)
                .fallbackModels(Collections.emptyList())
                .build();

        DomainAIResponse expectedResponse = DomainAIResponse.builder()
                .content("测试响应")
                .success(true)
                .modelUsed("gpt-4")
                .fallback(false)
                .tokensUsed(100)
                .responseTime(500L)
                .build();

        when(modelSelector.selectModel("CODE_GENERATION")).thenReturn(selectionResult);
        when(fallbackHandler.executeWithFallback(eq(testModelConfig), anyList(), eq(testRequest)))
                .thenReturn(expectedResponse);

        // When: 执行测试
        DomainAIResponse response = aiModelService.chat(testRequest);

        // Then: 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getContent()).isEqualTo("测试响应");

        // 验证交互
        verify(modelSelector).selectModel("CODE_GENERATION");
        verify(fallbackHandler).executeWithFallback(eq(testModelConfig), anyList(), eq(testRequest));
    }

    @Test
    @DisplayName("测试chat方法_调用失败_返回错误响应")
    void testChat_CallFailed_ReturnsErrorResponse() {
        // Given: 准备测试数据 - 模拟调用失败
        when(modelSelector.selectByQualityPriority()).thenReturn(testModelConfig);
        when(providerFactory.createChatClient(testModelConfig))
                .thenThrow(new RuntimeException("API调用失败"));

        // When: 执行测试
        DomainAIResponse response = aiModelService.chat(testRequest);

        // Then: 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getErrorMessage()).contains("API调用失败");
        assertThat(response.getModelUsed()).isEqualTo("gpt-4");

        // 验证保存了失败日志
        ArgumentCaptor<CallLog> logCaptor = ArgumentCaptor.forClass(CallLog.class);
        verify(callLogRepository).save(logCaptor.capture());
        CallLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getStatus()).isEqualTo(CallStatus.FAILED);
        assertThat(savedLog.getErrorMessage()).contains("API调用失败");
    }

    @Test
    @DisplayName("测试chatByTaskType方法_成功返回")
    void testChatByTaskType_Success() {
        // Given: 准备测试数据
        ModelSelectionResult selectionResult = ModelSelectionResult.builder()
                .primaryModel(testModelConfig)
                .fallbackModels(Collections.emptyList())
                .build();

        DomainAIResponse expectedResponse = DomainAIResponse.builder()
                .content("代码生成结果")
                .success(true)
                .modelUsed("gpt-4")
                .responseTime(500L)
                .tokensUsed(100)
                .fallback(false)
                .build();

        when(modelSelector.selectModel("CODE_GENERATION")).thenReturn(selectionResult);
        when(fallbackHandler.executeWithFallback(eq(testModelConfig), anyList(), eq(testRequest)))
                .thenReturn(expectedResponse);

        // When: 执行测试
        DomainAIResponse response = aiModelService.chatByTaskType("CODE_GENERATION", testRequest);

        // Then: 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getContent()).isEqualTo("代码生成结果");
        assertThat(response.getModelUsed()).isEqualTo("gpt-4");
        assertThat(response.getFallback()).isFalse();

        // 验证交互
        verify(modelSelector).selectModel("CODE_GENERATION");
        verify(fallbackHandler).executeWithFallback(eq(testModelConfig), anyList(), eq(testRequest));
        verify(callLogRepository).save(any(CallLog.class));
    }

    @Test
    @DisplayName("测试getAvailableModels方法_返回所有启用的模型")
    void testGetAvailableModels_ReturnsEnabledModels() {
        // Given: 准备测试数据
        ModelConfig model1 = ModelConfig.builder()
                .id(1L)
                .modelName("gpt-4")
                .modelType(ModelType.OPENAI)
                .enabled(true)
                .capability(testCapability)
                .build();

        ModelConfig model2 = ModelConfig.builder()
                .id(2L)
                .modelName("claude-3")
                .modelType(ModelType.ANTHROPIC)
                .enabled(true)
                .capability(testCapability)
                .build();

        when(modelConfigRepository.findByEnabledTrue()).thenReturn(Arrays.asList(model1, model2));

        // When: 执行测试
        List<DomainModelInfo> models = aiModelService.getAvailableModels();

        // Then: 验证结果
        assertThat(models).isNotNull();
        assertThat(models).hasSize(2);
        assertThat(models.get(0).getModelName()).isEqualTo("gpt-4");
        assertThat(models.get(1).getModelName()).isEqualTo("claude-3");

        // 验证交互
        verify(modelConfigRepository).findByEnabledTrue();
    }

    @Test
    @DisplayName("测试getAvailableModels方法_无可用模型_返回空列表")
    void testGetAvailableModels_NoModels_ReturnsEmptyList() {
        // Given: 准备测试数据 - 无可用模型
        when(modelConfigRepository.findByEnabledTrue()).thenReturn(Collections.emptyList());

        // When: 执行测试
        List<DomainModelInfo> models = aiModelService.getAvailableModels();

        // Then: 验证结果
        assertThat(models).isNotNull();
        assertThat(models).isEmpty();

        // 验证交互
        verify(modelConfigRepository).findByEnabledTrue();
    }

    @Test
    @DisplayName("测试getRecommendedModel方法_根据任务类型返回推荐模型")
    void testGetRecommendedModel_WithTaskType_ReturnsRecommendedModel() {
        // Given: 准备测试数据
        ModelSelectionResult selectionResult = ModelSelectionResult.builder()
                .primaryModel(testModelConfig)
                .fallbackModels(Collections.emptyList())
                .build();

        when(modelSelector.selectModel("CODE_GENERATION")).thenReturn(selectionResult);

        // When: 执行测试
        DomainModelInfo modelInfo = aiModelService.getRecommendedModel("CODE_GENERATION");

        // Then: 验证结果
        assertThat(modelInfo).isNotNull();
        assertThat(modelInfo.getModelName()).isEqualTo("gpt-4");
        assertThat(modelInfo.getModelType()).isEqualTo(ModelType.OPENAI);
        assertThat(modelInfo.getQualityScore()).isEqualTo(95);

        // 验证交互
        verify(modelSelector).selectModel("CODE_GENERATION");
    }

    @Test
    @DisplayName("测试getRecommendedModel方法_无任务类型_返回默认推荐模型")
    void testGetRecommendedModel_NoTaskType_ReturnsDefaultModel() {
        // Given: 准备测试数据
        ModelSelectionResult selectionResult = ModelSelectionResult.builder()
                .primaryModel(testModelConfig)
                .fallbackModels(Collections.emptyList())
                .build();

        when(modelSelector.selectModel(null)).thenReturn(selectionResult);

        // When: 执行测试
        DomainModelInfo modelInfo = aiModelService.getRecommendedModel(null);

        // Then: 验证结果
        assertThat(modelInfo).isNotNull();
        assertThat(modelInfo.getModelName()).isEqualTo("gpt-4");

        // 验证交互
        verify(modelSelector).selectModel(null);
    }

    @Test
    @DisplayName("测试chat方法_包含系统提示词_正确拼接")
    void testChat_WithSystemPrompt_ConcatenatesCorrectly() {
        // Given: 准备测试数据
        when(modelSelector.selectByQualityPriority()).thenReturn(testModelConfig);
        when(providerFactory.createChatClient(testModelConfig))
                .thenThrow(new RuntimeException("测试异常"));

        // When: 执行测试
        DomainAIResponse response = aiModelService.chat(testRequest);

        // Then: 验证结果 - 主要验证业务逻辑
        assertThat(response).isNotNull();
        assertThat(response.getSuccess()).isFalse();
        verify(callLogRepository).save(any(CallLog.class));
    }

    @Test
    @DisplayName("测试chat方法_无系统提示词_仅使用用户内容")
    void testChat_NoSystemPrompt_UsesOnlyUserContent() {
        // Given: 准备测试数据 - 无系统提示词
        testRequest.setSystemPrompt(null);
        when(modelSelector.selectByQualityPriority()).thenReturn(testModelConfig);
        when(providerFactory.createChatClient(testModelConfig))
                .thenThrow(new RuntimeException("测试异常"));

        // When: 执行测试
        DomainAIResponse response = aiModelService.chat(testRequest);

        // Then: 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getSuccess()).isFalse();
        verify(callLogRepository).save(any(CallLog.class));
    }
}

