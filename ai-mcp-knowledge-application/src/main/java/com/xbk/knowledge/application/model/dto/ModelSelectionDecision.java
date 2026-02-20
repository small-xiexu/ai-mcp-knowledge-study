package com.xbk.knowledge.application.model.dto;

import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型选择决策
 * 用于描述最终选中的模型
 *
 * 设计模式：结果对象（Result Object）
 * 职责：应用层选择结果，用于统一驱动后续调用流程
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelSelectionDecision {

    /**
     * 直接选择的模型（仅在直接选择模型时使用）
     *
     * 为什么：显式指定模型时使用
     */
    private ModelConfig selectedModel;

    /**
     * 直接选择模型
     *
     * 为什么：构建直接选择模型的决策对象
     * 入参：模型配置
     * 出参：选择决策
     */
    public static ModelSelectionDecision byModel(ModelConfig modelConfig) {
        return ModelSelectionDecision.builder()
                .selectedModel(modelConfig)
                .build();
    }
}
