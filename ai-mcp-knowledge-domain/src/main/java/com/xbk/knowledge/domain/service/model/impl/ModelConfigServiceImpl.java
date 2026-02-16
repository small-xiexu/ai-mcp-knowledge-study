package com.xbk.knowledge.domain.service.model.impl;

import com.xbk.knowledge.domain.model.aggregate.model.ModelConfigAggregate;
import com.xbk.knowledge.domain.model.entity.ModelCapability;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.vo.common.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.model.ModelConfigPageQuery;
import com.xbk.knowledge.domain.model.vo.model.ModelNameQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypeQuery;
import com.xbk.knowledge.domain.repository.model.ModelConfigRepository;
import com.xbk.knowledge.domain.service.model.IModelConfigService;
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
     *
     * 为什么：统一分页口径，避免前端传参与仓储不一致
     * 入参：分页查询对象
     * 出参：分页结果
     */
    @Override
    public PageResult<ModelConfig> queryModelConfigPage(ModelConfigPageQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("分页查询条件不能为空");
        }
        int offset = query.getOffset() == null ? 0 : query.getOffset();
        int pageSize = query.getPageSize() == null ? 10 : query.getPageSize();
        /*
         * 目的：规范化分页参数，避免异常分页导致性能问题
         */
        ModelConfigPageQuery pageQuery = new ModelConfigPageQuery(offset, pageSize);
        List<ModelConfig> models = modelConfigRepository.findPageWithCapability(pageQuery);

        /*
         * 目的：查询总数以支持分页组件
         */
        long total = modelConfigRepository.countAll();

        /*
         * 目的：将偏移量转换为页码以保持响应一致
         */
        int pageNum = (offset / pageSize) + 1;

        return PageResult.of(models, total, pageNum, pageSize);
    }

    /**
     * 根据 ID 查询模型配置
     *
     * 为什么：统一详情查询入口，不存在时抛出明确异常
     * 入参：ID 查询对象
     * 出参：模型配置
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
     *
     * 为什么：创建时保证唯一性与聚合一致性
     * 入参：模型配置实体
     * 出参：创建后的模型配置
     */
    @Override
    public ModelConfig createModelConfig(ModelConfig modelConfig) {
        /*
         * 目的：校验模型名称唯一性，避免数据库异常
         */
        String modelName = modelConfig.getModelName();
        ModelNameQuery modelNameQuery = new ModelNameQuery(modelName);
        if (modelConfigRepository
                .findByModelName(modelNameQuery)
                .isPresent()) {
            throw new IllegalArgumentException("模型名称已存在：" + modelName);
        }

        /*
         * 目的：补齐创建/更新时间，保证审计字段一致
         */
        LocalDateTime now = LocalDateTime.now();
        modelConfig.setCreatedAt(now);
        modelConfig.setUpdatedAt(now);

        /*
         * 目的：能力配置属于同一聚合，需同步设置时间字段
         */
        if (modelConfig.getCapability() != null) {
            modelConfig
                    .getCapability()
                    .setCreatedAt(now);
            modelConfig
                    .getCapability()
                    .setUpdatedAt(now);
        }

        /*
         * 目的：以聚合形式保存，确保模型与能力一致落库
         */
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
     *
     * 为什么：更新前校验存在性与唯一性，保证聚合一致
     * 入参：模型配置实体
     * 出参：更新后的模型配置
     */
    @Override
    public ModelConfig updateModelConfig(ModelConfig modelConfig) {
        if (modelConfig.getId() == null) {
            throw new IllegalArgumentException("更新操作必须提供模型 ID");
        }

        /*
         * 目的：读取现有配置，确保更新基于最新数据
         */
        Long modelConfigId = modelConfig.getId();
        IdQuery idQuery = new IdQuery(modelConfigId);
        String notFoundMessage = "模型配置不存在，id: " + modelConfigId;
        Supplier<NotFoundException> exceptionSupplier = () -> new NotFoundException(notFoundMessage);
        ModelConfig existingConfig = modelConfigRepository
                .findById(idQuery)
                .orElseThrow(exceptionSupplier);

        /*
         * 目的：检查名称是否与其他模型冲突
         */
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

        /*
         * 目的：覆盖可更新字段，保持聚合一致性
         */
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

        /*
         * 目的：能力配置可能缺失，需补齐以保持聚合一致
         */
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

        /*
         * 目的：以聚合形式保存，确保模型与能力一致落库
         */
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
     *
     * 为什么：防止删除不存在的模型，确保操作语义清晰
     * 入参：ID 查询对象
     * 出参：无
     */
    @Override
    public void deleteModelConfig(IdQuery query) {
        if (query == null || query.getId() == null) {
            throw new IllegalArgumentException("模型 ID 不能为空");
        }
        Long id = query.getId();
        /*
         * 目的：先检查存在性，避免静默失败
         */
        IdQuery idQuery = new IdQuery(id);
        if (!modelConfigRepository.existsById(idQuery)) {
            throw new NotFoundException("模型配置不存在，id: " + id);
        }

        /*
         * 目的：执行删除，释放配置
         */
        modelConfigRepository.deleteById(idQuery);
    }

    /**
     * 启用模型
     *
     * 为什么：启用模型以供业务选择，并更新审计时间
     * 入参：ID 查询对象
     * 出参：启用后的模型配置
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

        /*
         * 目的：保存聚合，保持能力配置一致
         */
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
     *
     * 为什么：禁用模型以防误用，并更新审计时间
     * 入参：ID 查询对象
     * 出参：禁用后的模型配置
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

        /*
         * 目的：保存聚合，保持能力配置一致
         */
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
     *
     * 为什么：提供可用模型集合供选择与推荐
     * 入参：启用状态查询对象
     * 出参：模型列表
     */
    @Override
    public List<ModelConfig> queryEnabledModels(EnabledQuery query) {
        /*
         * 目的：未传入时默认查询启用模型
         */
        Boolean enabled = query == null || query.getEnabled() == null ? Boolean.TRUE : query.getEnabled();
        EnabledQuery enabledQuery = new EnabledQuery(enabled);
        return modelConfigRepository.findByEnabled(enabledQuery);
    }

    /**
     * 获取推荐模型
     *
     * 为什么：提供默认推荐策略，后续可扩展
     * 入参：任务类型查询对象
     * 出参：推荐模型
     */
    @Override
    public ModelConfig getRecommendedModel(TaskTypeQuery query) {
        /*
         * 目的：当前使用启用模型兜底策略
         */
        EnabledQuery enabledQuery = new EnabledQuery(true);
        List<ModelConfig> models = modelConfigRepository.findByEnabled(enabledQuery);

        /*
         * 约束：目前返回第一个启用模型，后续可按任务类型推荐
         */
        return models.isEmpty() ? null : models.get(0);
    }
}
