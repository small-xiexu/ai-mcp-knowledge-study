package com.xbk.knowledge.infrastructure.repository.model;

import com.xbk.knowledge.domain.llm.model.aggregate.ModelConfigAggregate;
import com.xbk.knowledge.domain.llm.adapter.repository.ModelConfigRepository;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.common.model.valobj.EnabledIdsQuery;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.llm.model.valobj.ModelConfigPageQuery;
import com.xbk.knowledge.domain.llm.model.valobj.ModelNameQuery;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IModelConfigDao;
import com.xbk.knowledge.infrastructure.dao.po.ModelConfigPO;
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
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class ModelConfigRepositoryImpl implements ModelConfigRepository {

    /**
     * 模型配置数据访问对象。
     */
    private final IModelConfigDao modelConfigMapper;

    /**
     * 按启用状态查询模型列表
     * 用于配置管理列表展示
     *
     * 按启用状态过滤配置
     * 
     * @param query 启用状态查询条件。
     * @return 模型配置列表。
     */
    @Override
    public List<ModelConfig> findByEnabled(EnabledQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(modelConfigMapper.findByEnabled(query), ModelConfig.class);
    }

    /**
     * 查询所有启用模型
     * 用于推荐模型与可用模型列表
     *
     * 获取全量可用模型
     * 
     * @return 模型配置列表。
     */
    @Override
    public List<ModelConfig> findByEnabledTrue() {
        return BeanMappingUtils.mapList(modelConfigMapper.findEnabledTrue(), ModelConfig.class);
    }

    /**
     * 根据模型名称查询
     * 用于唯一性校验与快速定位
     *
     * 用于名称唯一性校验
     * 
     * @param query 模型名称查询条件。
     * @return 可选的模型配置。
     */
    @Override
    public Optional<ModelConfig> findByModelName(ModelNameQuery query) {
        if (query == null || query.getModelName() == null) {
            return Optional.empty();
        }
        ModelConfig modelConfig = BeanMappingUtils.map(modelConfigMapper.findByModelName(query), ModelConfig.class);
        return Optional.ofNullable(modelConfig);
    }

    /**
     * 按 ID 列表查询启用模型
     * 用于指定模型集合的备用模型解析
     *
     * 按指定 ID 过滤可用模型
     * 
     * @param query 启用模型 ID 集合查询条件。
     * @return 模型配置列表。
     */
    @Override
    public List<ModelConfig> findEnabledByIds(EnabledIdsQuery query) {
        if (query == null || query.getIds() == null || query.getIds().isEmpty()) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(modelConfigMapper.findEnabledByIds(query), ModelConfig.class);
    }

    /**
     * 根据 ID 查询模型配置
     * 用于详情展示与编辑加载
     *
     * 单条配置查询
     * 
     * @param query 主键查询条件。
     * @return 可选的模型配置。
     */
    @Override
    public Optional<ModelConfig> findById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return Optional.empty();
        }
        ModelConfig modelConfig = BeanMappingUtils.map(modelConfigMapper.findById(query), ModelConfig.class);
        return Optional.ofNullable(modelConfig);
    }

    /**
     * 分页查询模型配置
     * 用于配置管理分页展示
     *
     * 分页展示模型配置
     * 
     * @param query 分页查询条件。
     * @return 模型配置列表。
     */
    @Override
    public List<ModelConfig> findPage(ModelConfigPageQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(modelConfigMapper.findPage(query), ModelConfig.class);
    }

    /**
     * 统计模型配置总数
     * 用于分页统计
     *
     * 分页展示需要总数
     * 
     * @return 统计数量。
     */
    @Override
    public long countAll() {
        return modelConfigMapper.countAll();
    }

    /**
     * 判断模型是否存在
     * 用于删除与更新前置校验
     *
     * 避免更新/删除不存在的数据
     * 
     * @param query 主键查询条件。
     * @return `true` 表示模型存在，`false` 表示模型不存在。
     */
    @Override
    public boolean existsById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return false;
        }
        return modelConfigMapper.findById(query) != null;
    }

    /**
     * 保存模型配置
     * 统一插入与更新逻辑
     *
     * 统一处理模型配置落库逻辑
     * 
     * @param aggregate 模型配置聚合数据。
     * @return 保存后的模型配置聚合数据。
     */
    @Override
    public ModelConfigAggregate save(ModelConfigAggregate aggregate) {
        if (aggregate == null || aggregate.getModelConfig() == null) {
            return aggregate;
        }
        ModelConfig modelConfig = aggregate.getModelConfig();
        LocalDateTime now = LocalDateTime.now();
        if (modelConfig.getId() == null) {
            if (modelConfig.getCreatedAt() == null) {
                modelConfig.setCreatedAt(now);
            }
            if (modelConfig.getUpdatedAt() == null) {
                modelConfig.setUpdatedAt(now);
            }
            modelConfigMapper.insertModelConfig(BeanMappingUtils.map(modelConfig, ModelConfigPO.class));
            aggregate.setModelConfig(modelConfig);
            return aggregate;
        }
        if (modelConfig.getUpdatedAt() == null) {
            modelConfig.setUpdatedAt(now);
        }
        modelConfigMapper.updateModelConfig(BeanMappingUtils.map(modelConfig, ModelConfigPO.class));
        aggregate.setModelConfig(modelConfig);
        return aggregate;
    }

    /**
     * 根据 ID 删除模型配置
     *
     * 删除无效配置
     * 
     * @param query 主键查询条件。
     */
    @Override
    public void deleteById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return;
        }
        modelConfigMapper.deleteModelConfigById(query);
    }
}
