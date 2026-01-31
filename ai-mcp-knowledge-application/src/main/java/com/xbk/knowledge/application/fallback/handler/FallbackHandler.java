package com.xbk.knowledge.application.fallback.handler;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.fallback.executor.FailoverExecutor;
import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 降级处理器
 * 通过模板方法与迭代器封装降级流程，隐藏循环驱动细节
 * <p>
 * 设计模式：模板方法 + 迭代器（由执行器内部体现）
 * 职责：应用层容错入口，用于统一触发降级执行器
 *
 * @author xiexu
 */
@Service
@RequiredArgsConstructor
public class FallbackHandler {

    private final FailoverExecutor failoverExecutor;

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

        return failoverExecutor.execute(primary, fallbacks, request);
    }
}
