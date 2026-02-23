package com.xbk.knowledge.infrastructure.repository.model;

import com.xbk.knowledge.domain.llm.model.entity.ModelActivation;
import com.xbk.knowledge.domain.llm.adapter.repository.ModelActivationRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IModelActivationDao;
import com.xbk.knowledge.infrastructure.dao.po.ModelActivationPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 模型激活配置仓储实现
 *
 * 职责：模型激活数据持久化访问
 * @author sxie
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
        return BeanMappingUtils.map(modelActivationMapper.findActivation(), ModelActivation.class);
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
        ModelActivation existing = BeanMappingUtils.map(modelActivationMapper.findActivation(), ModelActivation.class);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            // 首次创建时补齐时间戳
            activation.setCreatedAt(now);
            activation.setUpdatedAt(now);
            modelActivationMapper.insertActivation(BeanMappingUtils.map(activation, ModelActivationPO.class));
            return;
        }
        // 沿用原 ID 与创建时间，仅更新更新时间
        activation.setId(existing.getId());
        activation.setCreatedAt(existing.getCreatedAt());
        activation.setUpdatedAt(now);
        modelActivationMapper.updateActivation(BeanMappingUtils.map(activation, ModelActivationPO.class));
    }
}
