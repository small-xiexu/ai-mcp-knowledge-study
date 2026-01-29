package com.xbk.knowledge.application.model.dto;

import com.xbk.knowledge.domain.model.entity.ModelConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 模型选择结果
 * 包含主模型和备用模型列表
 *
 * 职责：应用层命令/结果模型，用于传递用例输入输出
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelSelectionResult {

    /**
     * 主模型（首选模型）
     */
    private ModelConfig primaryModel;

    /**
     * 备用模型列表
     */
    private List<ModelConfig> fallbackModels;
}
