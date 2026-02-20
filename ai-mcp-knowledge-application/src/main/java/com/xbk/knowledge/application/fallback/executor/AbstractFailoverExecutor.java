package com.xbk.knowledge.application.fallback.executor;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.application.fallback.core.ModelCallOutcome;
import com.xbk.knowledge.application.fallback.plan.FailoverCandidate;
import com.xbk.knowledge.application.fallback.plan.FailoverPlan;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 抽象降级执行器
 * 固定降级流程骨架，细节交由子类实现
 *
 * 设计模式：模板方法（Template Method）
 * 职责：应用层模板方法，用于统一降级流程并降低阅读成本
 * @author sxie
 */
@Slf4j
public abstract class AbstractFailoverExecutor implements FailoverExecutor {

    /**
     * 对外暴露 execute 作为调用入口，便于上层复用。
     */
    @Override
    public AICallResult execute(ModelConfig primary, List<ModelConfig> fallbacks, AICallCommand request) {
        
        FailoverPlan plan = buildPlan(primary, fallbacks, request);

        String primaryName = plan.getPrimaryName();
        int fallbackCount = plan.getFallbackCount();
        log.info("开始执行带降级的调用，主模型: {}, 备用模型数量: {}",
                primaryName, fallbackCount);

        while (plan.hasNext()) {
            FailoverCandidate candidate = plan.next();
            ModelCallOutcome outcome = executeCandidate(candidate, request);

            if (outcome != null && outcome.isSuccess()) {
                AICallResult result = outcome.getResult();
                if (result != null && candidate.isFallback()) {
                    result.setFallback(true);
                }
                return result;
            }
        }

        log.error("所有模型调用均失败，主模型: {}, 备用模型数量: {}",
                primaryName, fallbackCount);

        return buildFailureResult();
    }

    /**
     * 构建降级计划
     *
     * @param primary   主模型
     * @param fallbacks 备用模型
     * @param request   请求参数
     * @return 降级计划
     */
    protected abstract FailoverPlan buildPlan(ModelConfig primary, List<ModelConfig> fallbacks, AICallCommand request);

    /**
     * 执行候选模型
     *
     * @param candidate 候选模型
     * @param request   请求参数
     * @return 执行结果
     */
    protected abstract ModelCallOutcome executeCandidate(FailoverCandidate candidate, AICallCommand request);

    /**
     * 构建失败结果
     *
     * @return 失败结果
     */
    protected AICallResult buildFailureResult() {
        return AICallResult.builder()
                .success(false)
                .errorMessage("所有模型调用均失败")
                .fallback(true)
                .build();
    }
}
