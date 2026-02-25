package com.xbk.knowledge.domain.llm.adapter.repository;

import com.xbk.knowledge.domain.llm.model.aggregate.ModelConfigAggregate;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.common.model.valobj.EnabledIdsQuery;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.llm.model.valobj.ModelConfigPageQuery;
import com.xbk.knowledge.domain.llm.model.valobj.ModelNameQuery;
import java.util.List;
import java.util.Optional;

/**
 * 模型配置仓储接口
 * 通过仓储抽象隔离数据访问实现
 *
 * 职责：领域仓储接口，用于屏蔽存储细节
 * @author sxie
 */
public interface ModelConfigRepository {

    /**
     * 查询指定启用状态的模型配置
     *
     * 按启用状态过滤模型配置
     * 
     * @param query 启用状态查询条件。
     * @return 模型配置列表。
     */
    List<ModelConfig> findByEnabled(EnabledQuery query);

    /**
     * 查询所有启用的模型配置
     *
     * 获取全量可用模型配置
     * 
     * @return 模型配置列表。
     */
    List<ModelConfig> findByEnabledTrue();

    /**
     * 根据模型名称查询模型配置
     *
     * 名称用于唯一性校验与定位配置
     * 
     * @param query 模型名称查询条件。
     * @return 可选的模型配置。
     */
    Optional<ModelConfig> findByModelName(ModelNameQuery query);

    /**
     * 根据ID列表查询启用的模型配置
     *
     * 按指定 ID 集合过滤可用模型
     * 
     * @param query 启用模型 ID 列表查询条件。
     * @return 模型配置列表。
     */
    List<ModelConfig> findEnabledByIds(EnabledIdsQuery query);

    /**
     * 根据ID查询模型配置
     *
     * 按唯一 ID 获取模型配置
     * 
     * @param query 主键查询条件。
     * @return 可选的模型配置。
     */
    Optional<ModelConfig> findById(IdQuery query);

    /**
     * 查询模型配置分页数据
     *
     * 分页展示模型配置
     * 
     * @param query 分页查询条件。
     * @return 模型配置列表。
     */
    List<ModelConfig> findPage(ModelConfigPageQuery query);

    /**
     * 统计模型配置总数
     *
     * 分页展示需要总数
     * 
     * @return 统计数量。
     */
    long countAll();

    /**
     * 判断模型配置是否存在
     *
     * 更新/删除前校验存在性
     * 
     * @param query 主键查询条件。
     * @return `true` 表示存在，`false` 表示不存在。
     */
    boolean existsById(IdQuery query);

    /**
     * 保存模型配置聚合（新增或更新）
     *
     * 保证模型配置聚合一致性
     * 
     * @param aggregate 模型配置聚合根。
     * @return 持久化后的模型配置聚合根。
     */
    ModelConfigAggregate save(ModelConfigAggregate aggregate);

    /**
     * 删除模型配置
     *
     * 移除无效配置
     * 
     * @param query 主键查询条件。
     */
    void deleteById(IdQuery query);
}
