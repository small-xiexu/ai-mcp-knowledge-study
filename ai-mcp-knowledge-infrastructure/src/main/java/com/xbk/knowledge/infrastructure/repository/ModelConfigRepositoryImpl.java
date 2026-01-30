package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.ModelCapability;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.vo.EnabledIdsQuery;
import com.xbk.knowledge.domain.model.vo.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.IdQuery;
import com.xbk.knowledge.domain.model.vo.ModelConfigPageQuery;
import com.xbk.knowledge.domain.model.vo.ModelNameQuery;
import com.xbk.knowledge.domain.model.vo.ModelTypeEnabledQuery;
import com.xbk.knowledge.domain.repository.ModelConfigRepository;
import com.xbk.knowledge.infrastructure.mapper.ModelCapabilityMapper;
import com.xbk.knowledge.infrastructure.mapper.ModelConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 模型配置仓储实现
 * 通过 Mapper 执行 XML SQL，隔离持久化细节
 *
 * 职责：仓储实现，用于落地数据访问
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class ModelConfigRepositoryImpl implements ModelConfigRepository {

    private final ModelConfigMapper modelConfigMapper;
    private final ModelCapabilityMapper modelCapabilityMapper;

    /**
     * 按模型类型与启用状态查询
     * 用于按维度过滤模型配置
     */
    @Override
    public List<ModelConfig> findByModelTypeAndEnabled(ModelTypeEnabledQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return modelConfigMapper.findByModelTypeAndEnabled(query);
    }

    /**
     * 按启用状态查询并按优先级排序
     * 用于模型选择的高优先级过滤
     */
    @Override
    public List<ModelConfig> findByEnabledOrderByPriorityDesc(EnabledQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return modelConfigMapper.findByEnabledOrderByPriorityDesc(query);
    }

    /**
     * 按启用状态查询模型列表
     * 用于配置管理列表展示
     */
    @Override
    public List<ModelConfig> findByEnabled(EnabledQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return modelConfigMapper.findByEnabled(query);
    }

    /**
     * 查询所有启用模型
     * 用于推荐模型与可用模型列表
     */
    @Override
    public List<ModelConfig> findByEnabledTrue() {
        return modelConfigMapper.findEnabledTrue();
    }

    /**
     * 根据模型名称查询
     * 用于唯一性校验与快速定位
     */
    @Override
    public Optional<ModelConfig> findByModelName(ModelNameQuery query) {
        if (query == null || query.getModelName() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(modelConfigMapper.findByModelName(query));
    }

    /**
     * 查询启用模型并带能力配置
     * 用于需要能力信息的模型选择场景
     */
    @Override
    public List<ModelConfig> findByEnabledTrueWithCapability() {
        return modelConfigMapper.findEnabledTrueWithCapability();
    }

    /**
     * 按 ID 列表查询启用模型
     * 用于任务类型的备用模型解析
     */
    @Override
    public List<ModelConfig> findEnabledByIds(EnabledIdsQuery query) {
        if (query == null || query.getIds() == null || query.getIds().isEmpty()) {
            return Collections.emptyList();
        }
        return modelConfigMapper.findEnabledByIds(query);
    }

    /**
     * 根据 ID 查询模型配置（含能力）
     * 用于详情展示与编辑加载
     */
    @Override
    public Optional<ModelConfig> findById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(modelConfigMapper.findByIdWithCapability(query));
    }

    /**
     * 分页查询模型配置（含能力）
     * 用于配置管理分页展示
     */
    @Override
    public List<ModelConfig> findPageWithCapability(ModelConfigPageQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return modelConfigMapper.findPageWithCapability(query);
    }

    /**
     * 统计模型配置总数
     * 用于分页统计
     */
    @Override
    public long countAll() {
        return modelConfigMapper.countAll();
    }

    /**
     * 判断模型是否存在
     * 用于删除与更新前置校验
     */
    @Override
    public boolean existsById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return false;
        }
        return modelConfigMapper.findById(query) != null;
    }

    /**
     * 保存模型配置与能力配置
     * 统一插入与更新逻辑，保证聚合一致性
     */
    @Override
    public ModelConfig save(ModelConfig modelConfig) {
        LocalDateTime now = LocalDateTime.now();
        if (modelConfig.getId() == null) {
            if (modelConfig.getCreatedAt() == null) {
                modelConfig.setCreatedAt(now);
            }
            if (modelConfig.getUpdatedAt() == null) {
                modelConfig.setUpdatedAt(now);
            }
            modelConfigMapper.insertModelConfig(modelConfig);
            persistCapability(modelConfig, now, true);
            return modelConfig;
        }
        if (modelConfig.getUpdatedAt() == null) {
            modelConfig.setUpdatedAt(now);
        }
        modelConfigMapper.updateModelConfig(modelConfig);
        persistCapability(modelConfig, now, false);
        return modelConfig;
    }

    /**
     * 根据 ID 删除模型配置
     * 同时清理能力配置，避免孤儿记录
     */
    @Override
    public void deleteById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return;
        }
        Long id = query.getId();
        modelCapabilityMapper.deleteByModelId(id);
        modelConfigMapper.deleteModelConfigById(query);
    }

    /**
     * 持久化能力配置
     * 根据是否仅插入决定新增或更新
     */
    private void persistCapability(ModelConfig modelConfig, LocalDateTime now, boolean insertOnly) {
        ModelCapability capability = modelConfig.getCapability();
        if (capability == null) {
            return;
        }
        if (capability.getModelId() == null) {
            capability.setModelId(modelConfig.getId());
        }
        if (insertOnly) {
            fillCapabilityCreateTime(capability, now);
            modelCapabilityMapper.insertModelCapability(capability);
            return;
        }
        ModelCapability existing = modelCapabilityMapper.findByModelId(capability.getModelId());
        if (existing == null) {
            fillCapabilityCreateTime(capability, now);
            modelCapabilityMapper.insertModelCapability(capability);
            return;
        }
        if (capability.getUpdatedAt() == null) {
            capability.setUpdatedAt(now);
        }
        modelCapabilityMapper.updateModelCapability(capability);
    }

    /**
     * 填充能力配置时间戳
     * 统一创建与更新时间的写入口径
     */
    private void fillCapabilityCreateTime(ModelCapability capability, LocalDateTime now) {
        if (capability.getCreatedAt() == null) {
            capability.setCreatedAt(now);
        }
        if (capability.getUpdatedAt() == null) {
            capability.setUpdatedAt(now);
        }
    }
}
