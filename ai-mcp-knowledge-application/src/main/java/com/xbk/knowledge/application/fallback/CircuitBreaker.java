package com.xbk.knowledge.application.fallback;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 熔断器
 * 连续失败后暂停使用该模型，避免雪崩效应
 *
 * 职责：应用层容错组件，用于提升调用稳定性
 * @author xiexu
 */
@Component
@Slf4j
public class CircuitBreaker {

    /**
     * 熔断阈值：连续失败3次触发熔断
     */
    private static final int FAILURE_THRESHOLD = 3;

    /**
     * 熔断恢复时间：5分钟（毫秒）
     */
    private static final long RECOVERY_TIMEOUT = 5 * 60 * 1000;

    /**
     * 熔断状态缓存
     * Key: 模型ID, Value: 熔断状态
     */
    private final Map<Long, CircuitState> circuitStates = new ConcurrentHashMap<>();

    /**
     * 检查熔断器是否打开
     * 如果熔断器打开，表示该模型暂时不可用
     *
     * @param modelId 模型ID
     * @return true-熔断器打开（不可用），false-熔断器关闭（可用）
     */
    public boolean isOpen(Long modelId) {
        CircuitState state = circuitStates.get(modelId);

        if (state == null) {
            return false;
        }

        // 检查是否到达恢复时间
        if (state.isOpen() && System.currentTimeMillis() - state.getOpenTime() > RECOVERY_TIMEOUT) {
            log.info("熔断器恢复，modelId: {}", modelId);
            // 尝试恢复：进入半开状态
            state.halfOpen();
            return false;
        }

        return state.isOpen();
    }

    /**
     * 记录成功调用
     * 成功后重置连续失败计数，关闭熔断器
     *
     * @param modelId 模型ID
     */
    public void recordSuccess(Long modelId) {
        CircuitState state = circuitStates.computeIfAbsent(modelId, k -> new CircuitState());
        state.recordSuccess();
        log.debug("记录成功调用，modelId: {}, consecutiveFailures: {}", modelId, state.getConsecutiveFailures());
    }

    /**
     * 记录失败调用
     * 连续失败达到阈值后打开熔断器
     *
     * @param modelId 模型ID
     */
    public void recordFailure(Long modelId) {
        CircuitState state = circuitStates.computeIfAbsent(modelId, k -> new CircuitState());
        state.recordFailure();

        log.warn("记录失败调用，modelId: {}, consecutiveFailures: {}", modelId, state.getConsecutiveFailures());

        // 达到失败阈值，打开熔断器
        if (state.getConsecutiveFailures() >= FAILURE_THRESHOLD) {
            state.open();
            log.error("熔断器打开，modelId: {}, consecutiveFailures: {}", modelId, state.getConsecutiveFailures());
        }
    }

    /**
     * 获取熔断状态（用于监控）
     *
     * @param modelId 模型ID
     * @return 熔断状态
     */
    public CircuitState getState(Long modelId) {
        return circuitStates.get(modelId);
    }

    /**
     * 熔断状态
     * 记录模型的熔断信息
     */
    @Data
    public static class CircuitState {
        /**
         * 连续失败次数
         */
        private int consecutiveFailures = 0;

        /**
         * 是否打开（熔断）
         */
        private boolean open = false;

        /**
         * 打开时间（用于计算恢复时间）
         */
        private long openTime = 0;

        /**
         * 记录成功调用
         * 重置失败计数，关闭熔断器
         */
        public void recordSuccess() {
            this.consecutiveFailures = 0;
            this.open = false;
        }

        /**
         * 记录失败调用
         * 增加失败计数
         */
        public void recordFailure() {
            this.consecutiveFailures++;
        }

        /**
         * 打开熔断器
         * 记录打开时间
         */
        public void open() {
            this.open = true;
            this.openTime = System.currentTimeMillis();
        }

        /**
         * 进入半开状态
         * 允许尝试一次调用，如果成功则关闭熔断器
         */
        public void halfOpen() {
            this.open = false;
            // 保持失败计数，如果再次失败会立即熔断
        }
    }
}
