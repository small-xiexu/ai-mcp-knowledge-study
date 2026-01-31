package com.xbk.knowledge.application.fallback.executor;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.application.fallback.core.ModelCallContext;
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
        /**
         * 从上下文中提前取出模型与请求信息，避免在调用流程中多次访问上下文，
         * 以保持调用链路的清晰度并减少后续逻辑分支中的重复取值。
         */
        ModelConfig model = context.getModel();
        AICallCommand request = context.getRequest();
        String modelName = model.getModelName();

        try {
            /**
             * 由工厂创建与模型配置匹配的 ChatClient，
             * 这样可以把具体供应商的细节隔离在工厂内部，调用层只关心统一接口。
             */
            ChatClient chatClient = providerFactory.createChatClient(model);

            /**
             * 将系统提示与用户内容合并为最终提示文本，
             * 统一输入格式可以减少不同模型接入时的差异处理成本。
             */
            String promptText = request.getSystemPrompt() != null
                    ? request.getSystemPrompt() + "\n\n" + request.getContent()
                    : request.getContent();

            /**
             * 通过链式调用完成一次请求并获取模型回复，
             * 这里集中执行调用，便于将异常捕获与结果封装放在同一边界。
             */
            String content = chatClient.prompt()
                    .user(promptText)
                    .call()
                    .content();

            /**
             * 构建成功结果并返回，统一由 AICallResult 承载，
             * 便于上层管道对成功与失败进行一致处理。
             */
            return AICallResult.builder()
                    .content(content)
                    .modelUsed(modelName)
                    .tokensUsed(0)
                    .success(true)
                    .retryCount(0)
                    .fallback(false)
                    .build();
        } catch (Exception e) {
            /**
             * 捕获所有异常并记录模型名称，方便定位具体模型或供应商的问题，
             * 同时将异常转为统一的错误消息对上层透出。
             */
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
