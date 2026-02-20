package com.xbk.knowledge.application.fallback.policy;

import com.xbk.knowledge.application.fallback.core.ModelCallContext;
import com.xbk.knowledge.application.fallback.core.ModelCallOutcome;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 日志策略
 * 统一记录模型调用过程，便于排查问题
 *
 * 设计模式：责任链节点（Logging Interceptor）
 * 职责：责任链中的观测节点，用于标准化日志输出
 * @author sxie
 */
@Component
@Slf4j
@Order(1)
public class LoggingPolicy extends AbstractModelCallPolicy {

    /**
     * 对外暴露 apply 作为调用入口，便于上层复用。
     */
    @Override
    public ModelCallOutcome apply(ModelCallContext context) {
        Long modelId = context
                .getModel()
                .getId();
        String modelName = context
                .getModel()
                .getModelName();

        log.info("开始执行模型调用，modelId: {}, modelName: {}", modelId, modelName);

        ModelCallOutcome outcome = next().apply(context);

        if (outcome.isSuccess()) {
            log.info("模型调用成功，modelId: {}, modelName: {}", modelId, modelName);
            return outcome;
        }

        if (outcome.isSkipped()) {
            log.warn("模型调用跳过，modelId: {}, modelName: {}", modelId, modelName);
            return outcome;
        }

        String errorMessage = outcome
                .getResult() != null ? outcome
                .getResult()
                .getErrorMessage() : "未知错误";
        log.warn("模型调用失败，modelId: {}, modelName: {}, error: {}", modelId, modelName, errorMessage);
        return outcome;
    }
}
