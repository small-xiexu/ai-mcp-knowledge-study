package com.xbk.knowledge.domain.llm.service.impl;

import com.xbk.knowledge.domain.llm.model.aggregate.ModelConfigAggregate;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.llm.model.valobj.ModelConfigPageQuery;
import com.xbk.knowledge.domain.llm.model.valobj.ModelNameQuery;
import com.xbk.knowledge.domain.llm.adapter.repository.ModelConfigRepository;
import com.xbk.knowledge.domain.llm.service.IModelConfigService;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.exception.NotFoundException;
import com.xbk.knowledge.types.enums.ModelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * 模型配置领域服务实现
 * 封装模型配置的业务逻辑
 *
 * 职责：领域服务实现，用于封装业务规则
 *
 * @author sxie
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelConfigServiceImpl implements IModelConfigService {
    /**
     * 模型配置仓储，用于模型配置读写与唯一性校验。
     */
    private final ModelConfigRepository modelConfigRepository;

    /**
     * 分页查询模型配置
     *
     * 统一分页口径，避免前端传参与仓储不一致
     * 
     * @param query 分页查询条件。
     * @return 模型配置分页结果。
     */
    @Override
    public PageResult<ModelConfig> queryModelConfigPage(ModelConfigPageQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("分页查询条件不能为空");
        }
        int offset = query.getOffset() == null ? 0 : query.getOffset();
        int pageSize = query.getPageSize() == null ? 10 : query.getPageSize();
        ModelConfigPageQuery pageQuery = new ModelConfigPageQuery(offset, pageSize);
        List<ModelConfig> models = modelConfigRepository.findPage(pageQuery);
        long total = modelConfigRepository.countAll();
        int pageNum = (offset / pageSize) + 1;
        return PageResult.of(models, total, pageNum, pageSize);
    }

    /**
     * 根据 ID 查询模型配置
     *
     * 统一详情查询入口，不存在时抛出明确异常
     * 
     * @param query 主键查询条件。
     * @return 模型配置详情。
     */
    @Override
    public ModelConfig queryModelConfigById(IdQuery query) {
        if (query == null || query.getId() == null) {
            throw new IllegalArgumentException("模型 ID 不能为空");
        }
        Long id = query.getId();
        IdQuery idQuery = new IdQuery(id);
        String notFoundMessage = "模型配置不存在，id: " + id;
        return modelConfigRepository
                .findById(idQuery)
                .orElseThrow(() -> new NotFoundException(notFoundMessage));
    }

    /**
     * 创建模型配置
     *
     * 创建时保证唯一性与聚合一致性
     * 
     * @param modelConfig 模型配置。
     * @return 创建后的模型配置。
     */
    @Override
    public ModelConfig createModelConfig(ModelConfig modelConfig) {
        String modelName = modelConfig.getModelName();
        ModelNameQuery modelNameQuery = new ModelNameQuery(modelName);
        if (modelConfigRepository.findByModelName(modelNameQuery).isPresent()) {
            throw new IllegalArgumentException("模型名称已存在" + modelName);
        }

        LocalDateTime now = LocalDateTime.now();
        modelConfig.setCreatedAt(now);
        modelConfig.setUpdatedAt(now);

        ModelConfigAggregate aggregate = ModelConfigAggregate.builder()
                .modelConfig(modelConfig)
                .build();
        ModelConfigAggregate savedAggregate = modelConfigRepository.save(aggregate);
        return savedAggregate.getModelConfig();
    }

    /**
     * 更新模型配置
     *
     * 更新前校验存在性与唯一性，保证聚合一致
     * 
     * @param modelConfig 模型配置。
     * @return 更新后的模型配置。
     */
    @Override
    public ModelConfig updateModelConfig(ModelConfig modelConfig) {
        if (modelConfig.getId() == null) {
            throw new IllegalArgumentException("更新操作必须提供模型 ID");
        }

        Long modelConfigId = modelConfig.getId();
        IdQuery idQuery = new IdQuery(modelConfigId);
        String notFoundMessage = "模型配置不存在，id: " + modelConfigId;
        ModelConfig existingConfig = modelConfigRepository
                .findById(idQuery)
                .orElseThrow(() -> new NotFoundException(notFoundMessage));

        String modelName = modelConfig.getModelName();
        ModelNameQuery modelNameQuery = new ModelNameQuery(modelName);
        Consumer<ModelConfig> duplicateChecker = existing -> {
            if (!existing.getId().equals(modelConfigId)) {
                throw new IllegalArgumentException("模型名称已存在" + modelName);
            }
        };
        modelConfigRepository.findByModelName(modelNameQuery).ifPresent(duplicateChecker);

        ModelType modelType = modelConfig.getModelType();
        String apiKey = modelConfig.getApiKey();
        String baseUrl = modelConfig.getBaseUrl();
        Boolean enabled = modelConfig.getEnabled();
        Boolean toolEnabled = modelConfig.getToolEnabled();
        Integer maxPromptChars = modelConfig.getMaxPromptChars();
        Integer maxHistoryMessages = modelConfig.getMaxHistoryMessages();
        LocalDateTime updatedAt = LocalDateTime.now();

        existingConfig.setModelName(modelName);
        existingConfig.setModelType(modelType);
        existingConfig.setApiKey(apiKey);
        existingConfig.setBaseUrl(baseUrl);
        existingConfig.setEnabled(enabled);
        existingConfig.setToolEnabled(toolEnabled);
        existingConfig.setMaxPromptChars(maxPromptChars);
        existingConfig.setMaxHistoryMessages(maxHistoryMessages);
        existingConfig.setUpdatedAt(updatedAt);

        ModelConfigAggregate aggregate = ModelConfigAggregate.builder()
                .modelConfig(existingConfig)
                .build();
        ModelConfigAggregate savedAggregate = modelConfigRepository.save(aggregate);
        return savedAggregate.getModelConfig();
    }

    /**
     * 删除模型配置
     *
     * 防止删除不存在的模型，确保操作语义清晰
     * 
     * @param query 主键查询条件。
     */
    @Override
    public void deleteModelConfig(IdQuery query) {
        if (query == null || query.getId() == null) {
            throw new IllegalArgumentException("模型 ID 不能为空");
        }
        Long id = query.getId();
        IdQuery idQuery = new IdQuery(id);
        if (!modelConfigRepository.existsById(idQuery)) {
            throw new NotFoundException("模型配置不存在，id: " + id);
        }
        modelConfigRepository.deleteById(idQuery);
    }

    /**
     * 启用模型
     *
     * 启用模型以供业务选择，并更新审计时间
     * 
     * @param query 主键查询条件。
     * @return 启用后的模型配置。
     */
    @Override
    public ModelConfig enableModel(IdQuery query) {
        if (query == null || query.getId() == null) {
            throw new IllegalArgumentException("模型 ID 不能为空");
        }
        Long id = query.getId();
        IdQuery idQuery = new IdQuery(id);
        String notFoundMessage = "模型配置不存在，id: " + id;
        ModelConfig modelConfig = modelConfigRepository
                .findById(idQuery)
                .orElseThrow(() -> new NotFoundException(notFoundMessage));

        modelConfig.setEnabled(true);
        modelConfig.setUpdatedAt(LocalDateTime.now());

        ModelConfigAggregate aggregate = ModelConfigAggregate.builder()
                .modelConfig(modelConfig)
                .build();
        ModelConfigAggregate savedAggregate = modelConfigRepository.save(aggregate);
        return savedAggregate.getModelConfig();
    }

    /**
     * 禁用模型
     *
     * 禁用模型以防误用，并更新审计时间
     * 
     * @param query 主键查询条件。
     * @return 禁用后的模型配置。
     */
    @Override
    public ModelConfig disableModel(IdQuery query) {
        if (query == null || query.getId() == null) {
            throw new IllegalArgumentException("模型 ID 不能为空");
        }
        Long id = query.getId();
        IdQuery idQuery = new IdQuery(id);
        String notFoundMessage = "模型配置不存在，id: " + id;
        ModelConfig modelConfig = modelConfigRepository
                .findById(idQuery)
                .orElseThrow(() -> new NotFoundException(notFoundMessage));

        modelConfig.setEnabled(false);
        modelConfig.setUpdatedAt(LocalDateTime.now());

        ModelConfigAggregate aggregate = ModelConfigAggregate.builder()
                .modelConfig(modelConfig)
                .build();
        ModelConfigAggregate savedAggregate = modelConfigRepository.save(aggregate);
        return savedAggregate.getModelConfig();
    }

    /**
     * 查询所有启用的模型
     *
     * 提供可用模型集合供选择与推荐
     * 
     * @param query 启用状态查询条件。
     * @return 模型配置列表。
     */
    @Override
    public List<ModelConfig> queryEnabledModels(EnabledQuery query) {
        Boolean enabled = query == null || query.getEnabled() == null ? Boolean.TRUE : query.getEnabled();
        EnabledQuery enabledQuery = new EnabledQuery(enabled);
        return modelConfigRepository.findByEnabled(enabledQuery);
    }
}
