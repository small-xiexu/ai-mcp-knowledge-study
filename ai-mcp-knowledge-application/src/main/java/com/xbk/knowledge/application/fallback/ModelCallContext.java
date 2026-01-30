package com.xbk.knowledge.application.fallback;

import com.xbk.knowledge.application.model.dto.AICallCommand;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型调用上下文
 * 统一携带模型配置与请求参数，便于策略组合传递
 *
 * 设计模式：上下文对象（Context Object）
 * 职责：应用层调用上下文，用于隔离流程与数据
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelCallContext {

    /**
     * 模型配置
     */
    private ModelConfig model;

    /**
     * 请求参数
     */
    private AICallCommand request;
}
