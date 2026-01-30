package com.xbk.knowledge.domain.service.impl;

import com.xbk.knowledge.domain.model.aggregate.model.ModelConfigAggregate;
import com.xbk.knowledge.domain.model.entity.ModelCapability;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.vo.common.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.model.ModelConfigPageQuery;
import com.xbk.knowledge.domain.model.vo.model.ModelNameQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypeQuery;
import com.xbk.knowledge.domain.repository.ModelConfigRepository;
import com.xbk.knowledge.domain.service.IModelConfigService;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.exception.NotFoundException;
import com.xbk.knowledge.types.enums.ModelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 模型配置领域服务实现
 * 封装模型配置的业务逻辑
 *
 * 职责：领域服务实现，用于封装业务规则
 * @author xiexu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelConfigServiceImpl implements IModelConfigService {

    private final ModelConfigRepository modelConfigRepository;

    /**
     * 分页查询模型配置
     * 统一分页口径并返回稳定的分页结构
     */
    @Override
    public PageResult<ModelConfig> queryModelConfigPage(ModelConfigPageQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("分页查询条件不能为空");
        }
        int offset = query.getOffset() == null ? 0 : query.getOffset();
        int pageSize = query.getPageSize() == null ? 10 : query.getPageSize();
        // 查询分页数据
        ModelConfigPageQuery pageQuery = new ModelConfigPageQuery(offset, pageSize);
        List<ModelConfig> models = modelConfigRepository.findPageWithCapability(pageQuery);

        // 查询总数
        long total = modelConfigRepository.countAll();

        // 计算页码
        int pageNum = (offset / pageSize) + 1;

        return PageResult.of(models, total, pageNum, pageSize);
    }

    /**
     * 根据 ID 查询模型配置
     * 明确业务语义，确保不存在时抛出领域异常
     */
    @Override
    public ModelConfig queryModelConfigById(IdQuery query) {
        if (query == null || query.getId() == null) {
            throw new IllegalArgumentException("模型 ID 不能为空");
        }
        Long id = query.getId();
        IdQuery idQuery = new IdQuery(id);
        String notFoundMessage = "模型配置不存在，id: " + id;
        Supplier<NotFoundException> exceptionSupplier = () -> new NotFoundException(notFoundMessage);
        return modelConfigRepository
                .findById(idQuery)
                .orElseThrow(exceptionSupplier);
    }

    /**
     * 创建模型配置
     * 统一校验并确保模型配置与能力配置的聚合一致性
     */
    @Override
    public ModelConfig createModelConfig(ModelConfig modelConfig) {
        // 检查模型名称是否已存在
        String modelName = modelConfig.getModelName();
        ModelNameQuery modelNameQuery = new ModelNameQuery(modelName);
        if (modelConfigRepository
                .findByModelName(modelNameQuery)
                .isPresent()) {
            // 业务层提前校验，避免数据库唯一约束异常影响可读性
            throw new IllegalArgumentException("模型名称已存在：" + modelName);
        }

        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        modelConfig.setCreatedAt(now);
        modelConfig.setUpdatedAt(now);

        // 能力配置属于同一聚合，确保与模型配置同时落库
        if (modelConfig.getCapability() != null) {
            modelConfig
                    .getCapability()
                    .setCreatedAt(now);
            modelConfig
                    .getCapability()
                    .setUpdatedAt(now);
        }

        // 保存到数据库
        ModelCapability modelCapability = modelConfig.getCapability();
        ModelConfigAggregate aggregate = ModelConfigAggregate.builder()
                .modelConfig(modelConfig)
                .modelCapability(modelCapability)
                .build();
        ModelConfigAggregate savedAggregate = modelConfigRepository.save(aggregate);
        return savedAggregate.getModelConfig();
    }

    /**
     * 更新模型配置
     * 校验唯一性与存在性，确保聚合内字段一致更新
     */
    @Override
    public ModelConfig updateModelConfig(ModelConfig modelConfig) {
        if (modelConfig.getId() == null) {
            throw new IllegalArgumentException("更新操作必须提供模型 ID");
        }

        // 查询现有配置
        Long modelConfigId = modelConfig.getId();
        IdQuery idQuery = new IdQuery(modelConfigId);
        String notFoundMessage = "模型配置不存在，id: " + modelConfigId;
        Supplier<NotFoundException> exceptionSupplier = () -> new NotFoundException(notFoundMessage);
        ModelConfig existingConfig = modelConfigRepository
                .findById(idQuery)
                .orElseThrow(exceptionSupplier);

        // 检查模型名称是否与其他模型冲突
        String modelName = modelConfig.getModelName();
        ModelNameQuery modelNameQuery = new ModelNameQuery(modelName);
        Consumer<ModelConfig> duplicateChecker = existing -> {
            if (!existing
                    .getId()
                    .equals(modelConfigId)) {
                throw new IllegalArgumentException("模型名称已存在：" + modelName);
            }
        };
        modelConfigRepository
                .findByModelName(modelNameQuery)
                .ifPresent(duplicateChecker);

        // 更新字段
        ModelType modelType = modelConfig.getModelType();
        String apiKey = modelConfig.getApiKey();
        String baseUrl = modelConfig.getBaseUrl();
        Boolean enabled = modelConfig.getEnabled();
        Integer priority = modelConfig.getPriority();
        LocalDateTime updatedAt = LocalDateTime.now();
        existingConfig.setModelName(modelName);
        existingConfig.setModelType(modelType);
        existingConfig.setApiKey(apiKey);
        existingConfig.setBaseUrl(baseUrl);
        existingConfig.setEnabled(enabled);
        existingConfig.setPriority(priority);
        existingConfig.setUpdatedAt(updatedAt);

        // 能力配置可能不存在（历史数据），此处补齐保证聚合一致性
        if (modelConfig.getCapability() != null) {
            ModelCapability configCapability = modelConfig.getCapability();
            ModelCapability capability = existingConfig.getCapability();
            if (capability == null) {
                capability = ModelCapability.builder()
                        .modelConfig(existingConfig)
                        .build();
                existingConfig.setCapability(capability);
            }
            Integer maxInputTokens = configCapability.getMaxInputTokens();
            Integer maxOutputTokens = configCapability.getMaxOutputTokens();
            Integer qualityScore = configCapability.getQualityScore();
            LocalDateTime capabilityUpdatedAt = LocalDateTime.now();
            capability.setMaxInputTokens(maxInputTokens);
            capability.setMaxOutputTokens(maxOutputTokens);
            capability.setQualityScore(qualityScore);
            capability.setUpdatedAt(capabilityUpdatedAt);
        }

        // 保存更新
        ModelCapability existingCapability = existingConfig.getCapability();
        ModelConfigAggregate aggregate = ModelConfigAggregate.builder()
                .modelConfig(existingConfig)
                .modelCapability(existingCapability)
                .build();
        ModelConfigAggregate savedAggregate = modelConfigRepository.save(aggregate);
        return savedAggregate.getModelConfig();
    }

    /**
     * 删除模型配置
     * 防止删除不存在的模型，确保操作语义清晰
     */
    @Override
    public void deleteModelConfig(IdQuery query) {
        if (query == null || query.getId() == null) {
            throw new IllegalArgumentException("模型 ID 不能为空");
        }
        Long id = query.getId();
        // 检查模型是否存在
        IdQuery idQuery = new IdQuery(id);
        if (!modelConfigRepository.existsById(idQuery)) {
            throw new NotFoundException("模型配置不存在，id: " + id);
        }

        // 删除模型
        modelConfigRepository.deleteById(idQuery);
    }

    /**
     * 启用模型
     * 统一更新启用状态并维护更新时间
     */
    @Override
    public ModelConfig enableModel(IdQuery query) {
        if (query == null || query.getId() == null) {
            throw new IllegalArgumentException("模型 ID 不能为空");
        }
        Long id = query.getId();
        IdQuery idQuery = new IdQuery(id);
        String notFoundMessage = "模型配置不存在，id: " + id;
        Supplier<NotFoundException> exceptionSupplier = () -> new NotFoundException(notFoundMessage);
        ModelConfig modelConfig = modelConfigRepository
                .findById(idQuery)
                .orElseThrow(exceptionSupplier);

        modelConfig.setEnabled(true);
        LocalDateTime updatedAt = LocalDateTime.now();
        modelConfig.setUpdatedAt(updatedAt);

        ModelCapability modelCapability = modelConfig.getCapability();
        ModelConfigAggregate aggregate = ModelConfigAggregate.builder()
                .modelConfig(modelConfig)
                .modelCapability(modelCapability)
                .build();
        ModelConfigAggregate savedAggregate = modelConfigRepository.save(aggregate);
        return savedAggregate.getModelConfig();
    }

    /**
     * 禁用模型
     * 统一更新禁用状态并维护更新时间
     */
    @Override
    public ModelConfig disableModel(IdQuery query) {
        if (query == null || query.getId() == null) {
            throw new IllegalArgumentException("模型 ID 不能为空");
        }
        Long id = query.getId();
        IdQuery idQuery = new IdQuery(id);
        String notFoundMessage = "模型配置不存在，id: " + id;
        Supplier<NotFoundException> exceptionSupplier = () -> new NotFoundException(notFoundMessage);
        ModelConfig modelConfig = modelConfigRepository
                .findById(idQuery)
                .orElseThrow(exceptionSupplier);

        modelConfig.setEnabled(false);
        LocalDateTime updatedAt = LocalDateTime.now();
        modelConfig.setUpdatedAt(updatedAt);

        ModelCapability modelCapability = modelConfig.getCapability();
        ModelConfigAggregate aggregate = ModelConfigAggregate.builder()
                .modelConfig(modelConfig)
                .modelCapability(modelCapability)
                .build();
        ModelConfigAggregate savedAggregate = modelConfigRepository.save(aggregate);
        return savedAggregate.getModelConfig();
    }

    /**
     * 查询所有启用的模型
     * 用于外部模型选择与推荐场景的基础数据来源
     */
    @Override
    public List<ModelConfig> queryEnabledModels(EnabledQuery query) {
        // 查询所有启用的模型
        Boolean enabled = query == null || query.getEnabled() == null ? Boolean.TRUE : query.getEnabled();
        EnabledQuery enabledQuery = new EnabledQuery(enabled);
        return modelConfigRepository.findByEnabled(enabledQuery);
    }

    /**
     * 获取推荐模型
     * 当前按启用模型兜底，预留基于任务类型的推荐策略
     */
    @Override
    public ModelConfig getRecommendedModel(TaskTypeQuery query) {
        // 查询所有启用的模型
        EnabledQuery enabledQuery = new EnabledQuery(true);
        List<ModelConfig> models = modelConfigRepository.findByEnabled(enabledQuery);

        // 返回第一个启用的模型作为推荐
        // TODO: 未来可以根据任务类型从 TaskType 表中查询 preferredModelId
        return models.isEmpty() ? null : models.get(0);
    }
}
