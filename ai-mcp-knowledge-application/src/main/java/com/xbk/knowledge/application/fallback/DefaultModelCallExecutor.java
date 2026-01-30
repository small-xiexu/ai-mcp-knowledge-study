package com.xbk.knowledge.application.fallback;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.application.provider.ModelProviderFactory;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.types.exception.ExceptionMessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * 默认模型调用执行器
 * 提供最小可用的单次调用实现
 *
 * 设计模式：策略实现（Strategy Implementation）
 * 职责：应用层调用实现，用于隔离模型调用细节
 * @author xiexu
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DefaultModelCallExecutor implements ModelCallExecutor {

    private final ModelProviderFactory providerFactory;

    /**
     * 对外暴露 execute 作为调用入口，便于上层复用。
     */
    @Override
    public AICallResult execute(ModelCallContext context) {
        ModelConfig model = context.getModel();
        AICallCommand request = context.getRequest();
        String modelName = model.getModelName();

        try {
            ChatClient chatClient = providerFactory.createChatClient(model);

            String promptText = request.getSystemPrompt() != null
                    ? request.getSystemPrompt() + "\n\n" + request.getContent()
                    : request.getContent();

            String content = chatClient.prompt()
                    .user(promptText)
                    .call()
                    .content();

            return AICallResult.builder()
                    .content(content)
                    .modelUsed(modelName)
                    .tokensUsed(0)
                    .success(true)
                    .retryCount(0)
                    .fallback(false)
                    .build();
        } catch (Exception e) {
            log.error("模型调用异常，modelName: {}", modelName, e);
            String errorMessage = ExceptionMessageUtils.resolveMessage(e, "调用失败", true);
            return AICallResult.builder()
                    .success(false)
                    .errorMessage(errorMessage)
                    .modelUsed(modelName)
                    .retryCount(0)
                    .fallback(false)
                    .build();
        }
    }
}
