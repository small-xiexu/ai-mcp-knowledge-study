package com.xbk.knowledge.application.fallback;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 默认降级执行器
 * 使用降级策略与调用管道执行候选模型
 *
 * 设计模式：模板方法实现（Template Method Implementation）
 * 职责：模板方法的默认实现，用于整合策略与管道并屏蔽遍历细节
 * @author xiexu
 */
@Component
@RequiredArgsConstructor
public class DefaultFailoverExecutor extends AbstractFailoverExecutor {

    private final FailoverStrategy failoverStrategy;
    private final ModelCallPipeline modelCallPipeline;

    @Override
    protected FailoverPlan buildPlan(ModelConfig primary, List<ModelConfig> fallbacks, AICallCommand request) {
        List<ModelConfig> candidates = failoverStrategy.orderCandidates(primary, fallbacks, request);
        return new DefaultFailoverPlan(candidates, primary, fallbacks);
    }

    @Override
    protected ModelCallOutcome executeCandidate(FailoverCandidate candidate, AICallCommand request) {
        ModelConfig model = candidate.getModel();
        ModelCallContext context = ModelCallContext.builder()
                .model(model)
                .request(request)
                .build();
        return modelCallPipeline.execute(context);
    }
}
