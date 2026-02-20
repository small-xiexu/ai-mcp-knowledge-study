package com.xbk.knowledge.application.service.selector;

import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.llm.adapter.repository.ModelConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 模型选择器
 * 负责选择可用模型
 *
 * 职责：应用层用例接口，用于定义编排能力
 *
 * @author sxie
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ModelSelector {

    private final ModelConfigRepository modelConfigRepository;

    /**
     * 默认策略
     * 选择第一个可用模型
     *
     * @return 可用模型配置
     */
    public ModelConfig selectByQualityPriority() {
        log.info("使用默认策略选择模型");

        List<ModelConfig> enabledModels = modelConfigRepository.findByEnabledTrue();
        if (enabledModels.isEmpty()) {
            throw new RuntimeException("没有可用的模型配置");
        }

        ModelConfig bestModel = enabledModels.get(0);

        log.info("默认策略选择完成，模型: {}", bestModel.getModelName());
        return bestModel;
    }
}
