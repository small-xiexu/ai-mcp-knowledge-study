package com.xbk.knowledge.application.fallback;

import com.xbk.knowledge.application.model.dto.AICallResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 重试策略
 * 通过有限重试提升瞬时失败的成功率
 *
 * 设计模式：责任链节点（Retry Interceptor）
 * 职责：责任链中的容错节点，用于控制重试行为
 * @author xiexu
 */
@Component
@Order(3)
@Slf4j
public class RetryPolicy implements ModelCallPolicy {

    /**
     * 默认最大重试次数
     */
    private static final int DEFAULT_MAX_RETRIES = 1;

    /**
     * 重试基准等待时间（毫秒）
     */
    private static final long BASE_BACKOFF_MILLIS = 100L;

    @Override
    public ModelCallOutcome apply(ModelCallContext context, ModelCallPolicyChain chain) {
        int retryCount = 0;
        ModelCallOutcome lastOutcome = null;

        while (retryCount <= DEFAULT_MAX_RETRIES) {
            ModelCallOutcome outcome = chain.proceed(context);
            lastOutcome = outcome;

            if (outcome.isSkipped()) {
                return outcome;
            }

            AICallResult result = outcome.getResult();
            Boolean success = result == null ? null : result.getSuccess();
            if (Boolean.TRUE.equals(success)) {
                result.setRetryCount(retryCount);
                return ModelCallOutcome.success(result);
            }

            if (retryCount >= DEFAULT_MAX_RETRIES) {
                String errorMessage = result != null ? result.getErrorMessage() : "调用失败";
                log.error("模型调用失败，已达最大重试次数，retryCount: {}, error: {}", retryCount, errorMessage);
                break;
            }

            retryCount++;
            String errorMessage = result != null ? result.getErrorMessage() : "调用失败";
            log.warn("模型调用失败，准备重试，retryCount: {}, error: {}", retryCount, errorMessage);
            try {
                long backoffMillis = BASE_BACKOFF_MILLIS * retryCount;
                Thread.sleep(backoffMillis);
            } catch (InterruptedException ie) {
                Thread
                        .currentThread()
                        .interrupt();
                break;
            }
        }

        if (lastOutcome != null && lastOutcome.getResult() != null) {
            lastOutcome
                    .getResult()
                    .setRetryCount(retryCount);
        }
        return lastOutcome != null ? lastOutcome : ModelCallOutcome
                .failed(AICallResult
                .builder()
                .success(false)
                .errorMessage("调用失败")
                .retryCount(retryCount)
                .fallback(false)
                .build());
    }
}
