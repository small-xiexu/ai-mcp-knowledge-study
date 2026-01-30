package com.xbk.knowledge.domain.service.impl;

import com.xbk.knowledge.domain.model.entity.ModelCapability;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.vo.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.IdQuery;
import com.xbk.knowledge.domain.model.vo.ModelConfigPageQuery;
import com.xbk.knowledge.domain.model.vo.ModelNameQuery;
import com.xbk.knowledge.domain.model.vo.TaskTypeQuery;
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
        List<ModelConfig> models = modelConfigRepository.findPageWithCapability(new ModelConfigPageQuery(offset, pageSize));

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
        return modelConfigRepository.findById(new IdQuery(id))
                .orElseThrow(() -> new NotFoundException("模型配置不存在，id: " + id));
    }

    /**
     * 创建模型配置
     * 统一校验并确保模型配置与能力配置的聚合一致性
     */
    @Override
    public ModelConfig createModelConfig(ModelConfig modelConfig) {
        // 检查模型名称是否已存在
        if (modelConfigRepository.findByModelName(new ModelNameQuery(modelConfig.getModelName())).isPresent()) {
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
        ModelConfig existingConfig = modelConfigRepository.findById(new IdQuery(modelConfig.getId()))
                .orElseThrow(() -> new NotFoundException("模型配置不存在，id: " + modelConfig.getId()));

        // 检查模型名称是否与其他模型冲突
        modelConfigRepository.findByModelName(new ModelNameQuery(modelConfig.getModelName()))
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
        if (!modelConfigRepository.existsById(new IdQuery(id))) {
            throw new NotFoundException("模型配置不存在，id: " + id);
        }

        // 删除模型
        modelConfigRepository.deleteById(new IdQuery(id));
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
        ModelConfig modelConfig = modelConfigRepository.findById(new IdQuery(id))
                .orElseThrow(() -> new NotFoundException("模型配置不存在，id: " + id));

        modelConfig.setEnabled(true);
        modelConfig.setUpdatedAt(LocalDateTime.now());

        return modelConfigRepository.save(modelConfig);
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
        ModelConfig modelConfig = modelConfigRepository.findById(new IdQuery(id))
                .orElseThrow(() -> new NotFoundException("模型配置不存在，id: " + id));

        modelConfig.setEnabled(false);
        modelConfig.setUpdatedAt(LocalDateTime.now());

        return modelConfigRepository.save(modelConfig);
    }

    /**
     * 查询所有启用的模型
     * 用于外部模型选择与推荐场景的基础数据来源
     */
    @Override
    public List<ModelConfig> queryEnabledModels(EnabledQuery query) {
        // 查询所有启用的模型
        Boolean enabled = query == null || query.getEnabled() == null ? Boolean.TRUE : query.getEnabled();
        return modelConfigRepository.findByEnabled(new EnabledQuery(enabled));
    }

    /**
     * 获取推荐模型
     * 当前按启用模型兜底，预留基于任务类型的推荐策略
     */
    @Override
    public ModelConfig getRecommendedModel(TaskTypeQuery query) {
        // 查询所有启用的模型
        List<ModelConfig> models = modelConfigRepository.findByEnabled(new EnabledQuery(true));

        // 返回第一个启用的模型作为推荐
        // TODO: 未来可以根据任务类型从 TaskType 表中查询 preferredModelId
        return models.isEmpty() ? null : models.get(0);
    }
}
