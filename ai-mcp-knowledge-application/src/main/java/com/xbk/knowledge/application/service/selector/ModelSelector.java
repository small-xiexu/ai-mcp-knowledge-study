package com.xbk.knowledge.application.service.selector;

import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.entity.TaskType;
import com.xbk.knowledge.domain.model.vo.common.EnabledIdsQuery;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypeCodeQuery;
import com.xbk.knowledge.domain.repository.model.ModelConfigRepository;
import com.xbk.knowledge.domain.repository.task.TaskTypeRepository;
import com.xbk.knowledge.application.model.dto.ModelSelectionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collector;
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
        TaskTypeCodeQuery taskTypeCodeQuery = new TaskTypeCodeQuery(taskType);
        TaskType task = taskTypeRepository.findByTaskCode(taskTypeCodeQuery).orElse(null);
        if (task == null) {
            log.warn("未找到任务类型配置，taskType: {}，使用质量优先策略", taskType);
            // 如果任务类型不存在，退化为“全局默认策略”，保证仍可对外服务
            ModelConfig primaryModel = selectByQualityPriority();
            List<ModelConfig> emptyFallbackModels = new ArrayList<>();
            return ModelSelectionResult.builder()
                    .primaryModel(primaryModel)
                    .fallbackModels(emptyFallbackModels)
                    .build();
        }

        // 2. 获取首选模型
        Long preferredModelId = task.getPreferredModelId();
        IdQuery idQuery = new IdQuery(preferredModelId);
        ModelConfig primaryModel = modelConfigRepository
                .findById(idQuery)
                .orElse(null);

        // 如果首选模型不存在或未启用，使用质量优先策略
        if (primaryModel == null || !primaryModel.getEnabled()) {
            log.warn("首选模型不存在或未启用，modelId: {}，使用质量优先策略", preferredModelId);
            primaryModel = selectByQualityPriority();
        }

        // 3. 解析并获取备用模型列表
        String fallbackModelIds = task.getFallbackModelIds();
        List<ModelConfig> fallbackModels = parseFallbackModels(fallbackModelIds);

        String primaryModelName = primaryModel.getModelName();
        int fallbackCount = fallbackModels.size();
        log.info("模型选择完成，首选模型: {}，备用模型数量: {}",
                primaryModelName, fallbackCount);

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

        // 按质量评分降序排序，选择最高分模型；若都没有能力信息则返回第一个
        ToIntFunction<ModelConfig> scoreExtractor = this::getQualityScoreOrZero;
        Comparator<ModelConfig> comparator = Comparator.comparingInt(scoreExtractor);
        Supplier<ModelConfig> fallbackSupplier = () -> enabledModels.get(0);
        ModelConfig bestModel = enabledModels
                .stream()
                .filter(model -> model.getCapability() != null)
                .max(comparator)
                .orElseGet(fallbackSupplier);

        String bestModelName = bestModel.getModelName();
        log.info("质量优先策略选择完成，模型: {}", bestModelName);
        return bestModel;
    }

    /**
     * 读取模型质量评分，避免空值影响比较
     *
     * @param model 模型配置
     * @return 质量评分（空值返回 0）
     */
    private int getQualityScoreOrZero(ModelConfig model) {
        if (model == null || model.getCapability() == null) {
            return 0;
        }
        Integer score = model
                .getCapability()
                .getQualityScore();
        return score != null ? score : 0;
    }

    /**
     * 解析备用模型ID列表
     * 从逗号分隔的字符串中解析出有效的备用模型列表
     *
     * @param fallbackModelIds 逗号分隔的模型ID字符串
     * @return 备用模型列表
     */
    private List<ModelConfig> parseFallbackModels(String fallbackModelIds) {
        if (fallbackModelIds == null || fallbackModelIds
                .trim()
                .isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // 解析ID列表
            Function<String, String> trimMapper = String::trim;
            Function<String, Long> idMapper = Long::parseLong;
            Collector<Long, ?, List<Long>> collector = Collectors.toList();
            String[] fallbackModelIdArray = fallbackModelIds.split(",");
            List<Long> modelIds = Arrays
                    .stream(fallbackModelIdArray)
                    .map(trimMapper)
                    .filter(s -> !s.isEmpty())
                    .map(idMapper)
                    .distinct()
                    .collect(collector);

            // 批量查询并过滤启用的模型（在 SQL 中直接过滤，提高性能）
            EnabledIdsQuery enabledIdsQuery = new EnabledIdsQuery(modelIds);
            return modelConfigRepository.findEnabledByIds(enabledIdsQuery);

        } catch (NumberFormatException e) {
            log.error("解析备用模型ID失败，fallbackModelIds: {}", fallbackModelIds, e);
            return new ArrayList<>();
        }
    }
}
