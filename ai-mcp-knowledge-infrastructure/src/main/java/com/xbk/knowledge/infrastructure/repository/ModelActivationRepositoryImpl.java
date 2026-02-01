package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.ModelActivation;
import com.xbk.knowledge.domain.repository.ModelActivationRepository;
import com.xbk.knowledge.infrastructure.mapper.ModelActivationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 模型激活配置仓储实现
 *
 * 职责：模型激活数据持久化访问
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class ModelActivationRepositoryImpl implements ModelActivationRepository {

    private final ModelActivationMapper modelActivationMapper;

    @Override
    public ModelActivation queryActivation() {
        return modelActivationMapper.findActivation();
    }

    @Override
    public ModelActivation saveOrUpdate(ModelActivation activation) {
        ModelActivation existing = modelActivationMapper.findActivation();
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            activation.setCreatedAt(now);
            activation.setUpdatedAt(now);
            modelActivationMapper.insertActivation(activation);
            return activation;
        }
        activation.setId(existing.getId());
        activation.setCreatedAt(existing.getCreatedAt());
        activation.setUpdatedAt(now);
        modelActivationMapper.updateActivation(activation);
        return activation;
    }
}
