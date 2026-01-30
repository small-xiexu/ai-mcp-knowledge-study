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
     */
    private boolean useTaskType;

    /**
     * 任务类型（仅在按任务类型分派时使用）
     */
    private String taskType;

    /**
     * 直接选择的模型（仅在直接选择模型时使用）
     */
    private ModelConfig selectedModel;

    /**
     * 直接选择模型
     *
     * @param modelConfig 模型配置
     * @return 选择决策
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
     * @param taskType 任务类型
     * @return 选择决策
     */
    public static ModelSelectionDecision byTaskType(String taskType) {
        return ModelSelectionDecision.builder()
                .useTaskType(true)
                .taskType(taskType)
                .build();
    }
}
