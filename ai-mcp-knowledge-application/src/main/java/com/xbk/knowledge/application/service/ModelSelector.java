package com.xbk.knowledge.application.service;

import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.entity.TaskType;
import com.xbk.knowledge.domain.model.vo.EnabledIdsQuery;
import com.xbk.knowledge.domain.model.vo.IdQuery;
import com.xbk.knowledge.domain.model.vo.TaskTypeCodeQuery;
import com.xbk.knowledge.domain.repository.ModelConfigRepository;
import com.xbk.knowledge.domain.repository.TaskTypeRepository;
import com.xbk.knowledge.application.model.dto.ModelSelectionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模型选择器
 * 根据任务类型和策略选择最优模型
 *
 * 职责：应用层用例接口，用于定义编排能力
 * @author xiexu
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ModelSelector {

    private final ModelConfigRepository modelConfigRepository;
    private final TaskTypeRepository taskTypeRepository;

    /**
     * 根据任务类型选择模型
     * 返回首选模型和备用模型列表
     *
     * @param taskType 任务类型编码
     * @return 模型选择结果（包含首选模型和备用模型列表）
     */
    public ModelSelectionResult selectModel(String taskType) {
        log.info("开始根据任务类型选择模型，taskType: {}", taskType);

        // 1. 查询任务类型配置
        TaskType task = taskTypeRepository.findByTaskCode(new TaskTypeCodeQuery(taskType)).orElse(null);
        if (task == null) {
            log.warn("未找到任务类型配置，taskType: {}，使用质量优先策略", taskType);
            // 如果任务类型不存在，退化为“全局默认策略”，保证仍可对外服务
            ModelConfig primaryModel = selectByQualityPriority();
            return ModelSelectionResult.builder()
                    .primaryModel(primaryModel)
                    .fallbackModels(new ArrayList<>())
                    .build();
        }

        // 2. 获取首选模型
        ModelConfig primaryModel = modelConfigRepository.findById(new IdQuery(task.getPreferredModelId())).orElse(null);

        // 如果首选模型不存在或未启用，使用质量优先策略
        if (primaryModel == null || !primaryModel.getEnabled()) {
            log.warn("首选模型不存在或未启用，modelId: {}，使用质量优先策略", task.getPreferredModelId());
            primaryModel = selectByQualityPriority();
        }

        // 3. 解析并获取备用模型列表
        List<ModelConfig> fallbackModels = parseFallbackModels(task.getFallbackModelIds());

        log.info("模型选择完成，首选模型: {}，备用模型数量: {}",
                primaryModel.getModelName(), fallbackModels.size());

        return ModelSelectionResult.builder()
                .primaryModel(primaryModel)
                .fallbackModels(fallbackModels)
                .build();
    }

    /**
     * 质量优先策略
     * 选择质量评分最高的可用模型
     *
     * @return 质量评分最高的模型配置
     */
    public ModelConfig selectByQualityPriority() {
        log.info("使用质量优先策略选择模型");

        // 使用 JOIN FETCH 一次性加载所有数据，避免 N+1 查询
        List<ModelConfig> enabledModels = modelConfigRepository.findByEnabledTrueWithCapability();

        if (enabledModels.isEmpty()) {
            throw new RuntimeException("没有可用的模型配置");
        }

        // 按质量评分降序排序，选择第一个
        ModelConfig bestModel = enabledModels.stream()
                .filter(model -> model.getCapability() != null)
                .max((m1, m2) -> {
                    Integer score1 = m1.getCapability().getQualityScore();
                    Integer score2 = m2.getCapability().getQualityScore();
                    return Integer.compare(
                            score1 != null ? score1 : 0,
                            score2 != null ? score2 : 0
                    );
                })
                .orElse(enabledModels.get(0)); // 如果都没有 capability，返回第一个

        log.info("质量优先策略选择完成，模型: {}", bestModel.getModelName());
        return bestModel;
    }

    /**
     * 解析备用模型ID列表
     * 从逗号分隔的字符串中解析出有效的备用模型列表
     *
     * @param fallbackModelIds 逗号分隔的模型ID字符串
     * @return 备用模型列表
     */
    private List<ModelConfig> parseFallbackModels(String fallbackModelIds) {
        if (fallbackModelIds == null || fallbackModelIds.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // 解析ID列表
            List<Long> modelIds = Arrays.stream(fallbackModelIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .distinct() // 去重
                    .collect(Collectors.toList());

            // 批量查询并过滤启用的模型（在 SQL 中直接过滤，提高性能）
            return modelConfigRepository.findEnabledByIds(new EnabledIdsQuery(modelIds));

        } catch (NumberFormatException e) {
            log.error("解析备用模型ID失败，fallbackModelIds: {}", fallbackModelIds, e);
            return new ArrayList<>();
        }
    }
}
