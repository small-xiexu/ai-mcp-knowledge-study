package com.xbk.knowledge.domain.service.impl;

import com.xbk.knowledge.domain.model.entity.ModelCapability;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.repository.ModelConfigRepository;
import com.xbk.knowledge.domain.service.IModelConfigService;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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

    @Override
    public PageResult<ModelConfig> queryModelConfigPage(int offset, int pageSize) {
        // 查询分页数据
        List<ModelConfig> models = modelConfigRepository.findPageWithCapability(offset, pageSize);

        // 查询总数
        long total = modelConfigRepository.countAll();

        // 计算页码
        int pageNum = (offset / pageSize) + 1;

        return PageResult.of(models, total, pageNum, pageSize);
    }

    @Override
    public ModelConfig queryModelConfigById(Long id) {
        return modelConfigRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("模型配置不存在，id: " + id));
    }

    @Override
    public ModelConfig createModelConfig(ModelConfig modelConfig) {
        // 检查模型名称是否已存在
        if (modelConfigRepository.findByModelName(modelConfig.getModelName()).isPresent()) {
            // 业务层提前校验，避免数据库唯一约束异常影响可读性
            throw new IllegalArgumentException("模型名称已存在：" + modelConfig.getModelName());
        }

        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        modelConfig.setCreatedAt(now);
        modelConfig.setUpdatedAt(now);

        // 能力配置属于同一聚合，确保与模型配置同时落库
        if (modelConfig.getCapability() != null) {
            modelConfig.getCapability().setCreatedAt(now);
            modelConfig.getCapability().setUpdatedAt(now);
        }

        // 保存到数据库
        return modelConfigRepository.save(modelConfig);
    }

    @Override
    public ModelConfig updateModelConfig(ModelConfig modelConfig) {
        if (modelConfig.getId() == null) {
            throw new IllegalArgumentException("更新操作必须提供模型 ID");
        }

        // 查询现有配置
        ModelConfig existingConfig = modelConfigRepository.findById(modelConfig.getId())
                .orElseThrow(() -> new NotFoundException("模型配置不存在，id: " + modelConfig.getId()));

        // 检查模型名称是否与其他模型冲突
        modelConfigRepository.findByModelName(modelConfig.getModelName())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(modelConfig.getId())) {
                        throw new IllegalArgumentException("模型名称已存在：" + modelConfig.getModelName());
                    }
                });

        // 更新字段
        existingConfig.setModelName(modelConfig.getModelName());
        existingConfig.setModelType(modelConfig.getModelType());
        existingConfig.setApiKey(modelConfig.getApiKey());
        existingConfig.setBaseUrl(modelConfig.getBaseUrl());
        existingConfig.setEnabled(modelConfig.getEnabled());
        existingConfig.setPriority(modelConfig.getPriority());
        existingConfig.setUpdatedAt(LocalDateTime.now());

        // 能力配置可能不存在（历史数据），此处补齐保证聚合一致性
        if (modelConfig.getCapability() != null) {
            ModelCapability capability = existingConfig.getCapability();
            if (capability == null) {
                capability = ModelCapability.builder()
                        .modelConfig(existingConfig)
                        .build();
                existingConfig.setCapability(capability);
            }
            capability.setMaxInputTokens(modelConfig.getCapability().getMaxInputTokens());
            capability.setMaxOutputTokens(modelConfig.getCapability().getMaxOutputTokens());
            capability.setQualityScore(modelConfig.getCapability().getQualityScore());
            capability.setUpdatedAt(LocalDateTime.now());
        }

        // 保存更新
        return modelConfigRepository.save(existingConfig);
    }

    @Override
    public void deleteModelConfig(Long id) {
        // 检查模型是否存在
        if (!modelConfigRepository.existsById(id)) {
            throw new NotFoundException("模型配置不存在，id: " + id);
        }

        // 删除模型
        modelConfigRepository.deleteById(id);
    }

    @Override
    public ModelConfig enableModel(Long id) {
        ModelConfig modelConfig = modelConfigRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("模型配置不存在，id: " + id));

        modelConfig.setEnabled(true);
        modelConfig.setUpdatedAt(LocalDateTime.now());

        return modelConfigRepository.save(modelConfig);
    }

    @Override
    public ModelConfig disableModel(Long id) {
        ModelConfig modelConfig = modelConfigRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("模型配置不存在，id: " + id));

        modelConfig.setEnabled(false);
        modelConfig.setUpdatedAt(LocalDateTime.now());

        return modelConfigRepository.save(modelConfig);
    }

    @Override
    public List<ModelConfig> queryEnabledModels() {
        // 查询所有启用的模型
        return modelConfigRepository.findByEnabled(true);
    }

    @Override
    public ModelConfig getRecommendedModel(String taskType) {
        // 查询所有启用的模型
        List<ModelConfig> models = modelConfigRepository.findByEnabled(true);

        // 返回第一个启用的模型作为推荐
        // TODO: 未来可以根据任务类型从 TaskType 表中查询 preferredModelId
        return models.isEmpty() ? null : models.get(0);
    }
}
