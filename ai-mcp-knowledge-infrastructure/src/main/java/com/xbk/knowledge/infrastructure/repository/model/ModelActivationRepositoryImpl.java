package com.xbk.knowledge.infrastructure.repository.model;

import com.xbk.knowledge.domain.model.entity.ModelActivation;
import com.xbk.knowledge.domain.model.adapter.repository.model.ModelActivationRepository;
import com.xbk.knowledge.infrastructure.dao.IModelActivationDao;
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

    private final IModelActivationDao modelActivationMapper;

    /**
     * 查询当前激活配置
     *
     * 为什么：全局只有一份激活配置
     * 入参：无
     * 出参：激活配置
     */
    @Override
    public ModelActivation queryActivation() {
        return modelActivationMapper.findActivation();
    }

    /**
     * 保存或更新激活配置
     * <p>
     * 为什么：激活配置可能不存在，需要支持新增与更新
     * 入参：激活配置
     * 出参：保存后的激活配置
     */
    @Override
    public void saveOrUpdate(ModelActivation activation) {
        ModelActivation existing = modelActivationMapper.findActivation();
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            /*
             * 目的：首次创建时补齐时间戳
             */
            activation.setCreatedAt(now);
            activation.setUpdatedAt(now);
            modelActivationMapper.insertActivation(activation);
            return;
        }
        /*
         * 目的：沿用原 ID 与创建时间，仅更新更新时间
         */
        activation.setId(existing.getId());
        activation.setCreatedAt(existing.getCreatedAt());
        activation.setUpdatedAt(now);
        modelActivationMapper.updateActivation(activation);
    }
}
