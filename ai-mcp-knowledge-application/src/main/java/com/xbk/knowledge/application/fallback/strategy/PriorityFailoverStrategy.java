package com.xbk.knowledge.application.fallback.strategy;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 优先级降级策略
 * 保持主模型优先，按顺序尝试备用模型
 *
 * 设计模式：策略实现（Strategy Implementation）
 * 职责：默认策略实现，用于维持现有业务语义
 * @author xiexu
 */
@Component
public class PriorityFailoverStrategy implements FailoverStrategy {

    /**
     * 对外暴露 orderCandidates 作为调用入口，便于上层复用。
     */
    @Override
    public List<ModelConfig> orderCandidates(ModelConfig primary, List<ModelConfig> fallbacks, AICallCommand request) {
        List<ModelConfig> candidates = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        Long primaryId = primary == null ? null : primary.getId();
        if (primaryId != null && seen.add(primaryId)) {
            candidates.add(primary);
        }

        if (fallbacks == null || fallbacks.isEmpty()) {
            return candidates;
        }

        for (ModelConfig fallback : fallbacks) {
            Long fallbackId = fallback == null ? null : fallback.getId();
            if (fallbackId == null) {
                continue;
            }
            if (seen.add(fallbackId)) {
                candidates.add(fallback);
            }
        }

        return candidates;
    }
}
