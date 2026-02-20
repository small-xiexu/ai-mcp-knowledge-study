package com.xbk.knowledge.application.fallback.executor;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.application.model.dto.AICallResult;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;

import java.util.List;

/**
 * 降级执行器
 * 负责执行降级流程并输出最终结果
 *
 * 设计模式：模板方法（Template Method）入口
 * 职责：应用层执行接口，用于隐藏流程细节
 * @author sxie
 */
public interface FailoverExecutor {

    /**
     * 执行降级调用
     *
     * @param primary   主模型
     * @param fallbacks 备用模型
     * @param request   请求参数
     * @return 调用结果
     */
    AICallResult execute(ModelConfig primary, List<ModelConfig> fallbacks, AICallCommand request);
}
