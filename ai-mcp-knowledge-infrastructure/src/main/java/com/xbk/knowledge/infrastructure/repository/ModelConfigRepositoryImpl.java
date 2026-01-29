package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.ModelCapability;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.repository.ModelConfigRepository;
import com.xbk.knowledge.infrastructure.mapper.ModelCapabilityMapper;
import com.xbk.knowledge.infrastructure.mapper.ModelConfigMapper;
import com.xbk.knowledge.types.enums.ModelType;
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

    @Override
    public List<ModelConfig> findByModelTypeAndEnabled(ModelType modelType, Boolean enabled) {
        return modelConfigMapper.findByModelTypeAndEnabled(modelType, enabled);
    }

    @Override
    public List<ModelConfig> findByEnabledOrderByPriorityDesc(Boolean enabled) {
        return modelConfigMapper.findByEnabledOrderByPriorityDesc(enabled);
    }

    @Override
    public List<ModelConfig> findByEnabled(Boolean enabled) {
        return modelConfigMapper.findByEnabled(enabled);
    }

    @Override
    public List<ModelConfig> findByEnabledTrue() {
        return modelConfigMapper.findEnabledTrue();
    }

    @Override
    public Optional<ModelConfig> findByModelName(String modelName) {
        return Optional.ofNullable(modelConfigMapper.findByModelName(modelName));
    }

    @Override
    public List<ModelConfig> findByEnabledTrueWithCapability() {
        return modelConfigMapper.findEnabledTrueWithCapability();
    }

    @Override
    public List<ModelConfig> findEnabledByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return modelConfigMapper.findEnabledByIds(ids);
    }

    @Override
    public Optional<ModelConfig> findById(Long id) {
        return Optional.ofNullable(modelConfigMapper.findByIdWithCapability(id));
    }

    @Override
    public List<ModelConfig> findPageWithCapability(int offset, int pageSize) {
        return modelConfigMapper.findPageWithCapability(offset, pageSize);
    }

    @Override
    public long countAll() {
        return modelConfigMapper.countAll();
    }

    @Override
    public boolean existsById(Long id) {
        return modelConfigMapper.findById(id) != null;
    }

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

    @Override
    public void deleteById(Long id) {
        if (id == null) {
            return;
        }
        modelCapabilityMapper.deleteByModelId(id);
        modelConfigMapper.deleteModelConfigById(id);
    }

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

    private void fillCapabilityCreateTime(ModelCapability capability, LocalDateTime now) {
        if (capability.getCreatedAt() == null) {
            capability.setCreatedAt(now);
        }
        if (capability.getUpdatedAt() == null) {
            capability.setUpdatedAt(now);
        }
    }
}
