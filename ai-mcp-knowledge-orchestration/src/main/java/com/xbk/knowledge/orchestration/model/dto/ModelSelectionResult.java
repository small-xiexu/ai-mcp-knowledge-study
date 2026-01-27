package com.xbk.knowledge.orchestration.model.dto;

import com.xbk.knowledge.orchestration.domain.entity.ModelConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 模型选择结果
 * 包含主模型和备用模型列表
 *
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
