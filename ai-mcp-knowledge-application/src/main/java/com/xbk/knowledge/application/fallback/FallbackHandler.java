package com.xbk.knowledge.application.fallback;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.application.provider.ModelProviderFactory;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 降级处理器
 * 实现重试和熔断机制，提供自动降级能力
 *
 * 职责：应用层容错组件，用于提升调用稳定性
 * @author xiexu
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FallbackHandler {

    private final CircuitBreaker circuitBreaker;
    private final ModelProviderFactory providerFactory;

    /**
     * 默认最大重试次数
     */
    private static final int DEFAULT_MAX_RETRIES = 1;

    /**
     * 执行带降级的调用
     * 主模型失败后自动尝试备用模型
     *
     * @param primary   主模型配置
     * @param fallbacks 备用模型列表
     * @param request   请求对象
     * @return 响应对象
     */
    public AICallResult executeWithFallback(
            ModelConfig primary,
            List<ModelConfig> fallbacks,
            AICallCommand request) {

        log.info("开始执行带降级的调用，主模型: {}, 备用模型数量: {}",
                primary.getModelName(), fallbacks.size());

        // 1. 检查主模型熔断状态：避免在已知不健康的模型上继续消耗请求
        if (circuitBreaker.isOpen(primary.getId())) {
            log.warn("主模型已熔断，跳过主模型，modelId: {}", primary.getId());
        } else {
            // 2. 尝试主模型（带重试）
            AICallResult response = executeWithRetry(primary, request, DEFAULT_MAX_RETRIES);

            if (response.getSuccess()) {
                circuitBreaker.recordSuccess(primary.getId());
                log.info("主模型调用成功，modelId: {}", primary.getId());
                return response;
            }

            // 3. 主模型失败，记录失败
            circuitBreaker.recordFailure(primary.getId());
            log.warn("主模型调用失败，modelId: {}, error: {}",
                    primary.getId(), response.getErrorMessage());
        }

        // 4. 尝试备用模型
        for (ModelConfig fallback : fallbacks) {
            // 检查熔断状态
            if (circuitBreaker.isOpen(fallback.getId())) {
                log.warn("备用模型已熔断，跳过，modelId: {}", fallback.getId());
                continue;
            }

            log.info("尝试备用模型，modelId: {}, modelName: {}",
                    fallback.getId(), fallback.getModelName());

            AICallResult response = executeWithRetry(fallback, request, DEFAULT_MAX_RETRIES);

            if (response.getSuccess()) {
                response.setFallback(true);
                circuitBreaker.recordSuccess(fallback.getId());
                log.info("备用模型调用成功，modelId: {}", fallback.getId());
                return response;
            }

            // 备用模型失败，记录失败（触发熔断阈值时将临时隔离）
            circuitBreaker.recordFailure(fallback.getId());
            log.warn("备用模型调用失败，modelId: {}, error: {}",
                    fallback.getId(), response.getErrorMessage());
        }

        // 5. 所有模型都失败
        log.error("所有模型调用均失败，主模型: {}, 备用模型数量: {}",
                primary.getModelName(), fallbacks.size());

        return AICallResult.builder()
                .success(false)
                .errorMessage("所有模型调用均失败")
                .fallback(true)
                .build();
    }

    /**
     * 执行带重试的调用
     * 失败后自动重试，直到达到最大重试次数
     *
     * @param model       模型配置
     * @param request     请求对象
     * @param maxRetries  最大重试次数
     * @return 响应对象
     */
    private AICallResult executeWithRetry(
            ModelConfig model,
            AICallCommand request,
            int maxRetries) {

        int retryCount = 0;
        Exception lastException = null;

        while (retryCount <= maxRetries) {
            try {
                long startTime = System.currentTimeMillis();

                // 创建 ChatClient
                ChatClient chatClient = providerFactory.createChatClient(model);

                // 构建 Prompt
                String promptText = request.getSystemPrompt() != null
                        ? request.getSystemPrompt() + "\n\n" + request.getContent()
                        : request.getContent();

                // 调用模型
                String content = chatClient.prompt()
                        .user(promptText)
                        .call()
                        .content();

                long responseTime = System.currentTimeMillis() - startTime;

                log.info("模型调用成功，modelId: {}, retryCount: {}, responseTime: {}ms",
                        model.getId(), retryCount, responseTime);

                return AICallResult.builder()
                        .content(content)
                        .modelUsed(model.getModelName())
                        .tokensUsed(0) // Spring AI 可能无法提供，设为 0
                        .responseTime(responseTime)
                        .success(true)
                        .retryCount(retryCount)
                        .build();

            } catch (Exception e) {
                lastException = e;
                retryCount++;

                if (retryCount <= maxRetries) {
                    log.warn("模型调用失败，准备重试，modelId: {}, retryCount: {}, error: {}",
                            model.getId(), retryCount, e.getMessage());

                    // 重试前等待一小段时间（简化版指数退避，降低瞬时压力）
                    try {
                        Thread.sleep(100L * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("模型调用失败，已达最大重试次数，modelId: {}, retryCount: {}, error: {}",
                            model.getId(), retryCount, e.getMessage());
                }
            }
        }

        // 所有重试都失败
        return AICallResult.builder()
                .success(false)
                .errorMessage(lastException != null ? lastException.getMessage() : "调用失败")
                .modelUsed(model.getModelName())
                .retryCount(retryCount - 1)
                .build();
    }
}
