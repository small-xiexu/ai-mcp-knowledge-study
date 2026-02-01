package com.xbk.knowledge.application.model.dto;

import com.xbk.knowledge.domain.model.entity.ModelConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型选择决策
 * 用于描述是直接选择模型还是按任务类型分派
 *
 * 设计模式：结果对象（Result Object）
 * 职责：应用层选择结果，用于统一驱动后续调用流程
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelSelectionDecision {

    /**
     * 是否按任务类型分派
     *
     * 为什么：控制后续选择路径
     */
    private boolean useTaskType;

    /**
     * 任务类型（仅在按任务类型分派时使用）
     *
     * 为什么：用于按任务类型选择模型
     */
    private String taskType;

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
                .useTaskType(false)
                .selectedModel(modelConfig)
                .build();
    }

    /**
     * 按任务类型分派
     *
     * 为什么：构建按任务类型分派的决策对象
     * 入参：任务类型
     * 出参：选择决策
     */
    public static ModelSelectionDecision byTaskType(String taskType) {
        return ModelSelectionDecision.builder()
                .useTaskType(true)
                .taskType(taskType)
                .build();
    }
}
