package com.xbk.knowledge.application.fallback;

import com.xbk.knowledge.application.model.dto.AICallResult;

/**
 * 模型调用执行器
 * 负责执行一次模型调用，不处理重试或降级逻辑
 *
 * 设计模式：策略接口（Strategy）
 * 职责：应用层执行组件，用于隔离底层调用细节
 * @author xiexu
 */
public interface ModelCallExecutor {

    /**
     * 执行一次调用
     *
     * @param context 调用上下文
     * @return 调用结果
     */
    AICallResult execute(ModelCallContext context);
}
