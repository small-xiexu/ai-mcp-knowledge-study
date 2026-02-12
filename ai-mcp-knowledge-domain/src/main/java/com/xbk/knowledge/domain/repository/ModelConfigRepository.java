package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.aggregate.model.ModelConfigAggregate;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.vo.common.EnabledIdsQuery;
import com.xbk.knowledge.domain.model.vo.common.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.model.ModelConfigPageQuery;
import com.xbk.knowledge.domain.model.vo.model.ModelNameQuery;
import java.util.List;
import java.util.Optional;

/**
 * 模型配置仓储接口
 * 通过仓储抽象隔离数据访问实现
 *
 * 职责：领域仓储接口，用于屏蔽存储细节
 * @author xiexu
 */
public interface ModelConfigRepository {

    /**
     * 查询启用模型配置并按优先级降序排序
     *
     * 为什么：优先级用于推荐与路由决策
     * 入参：启用状态查询条件
     * 出参：模型配置列表
     */
    List<ModelConfig> findByEnabledOrderByPriorityDesc(EnabledQuery query);

    /**
     * 查询指定启用状态的模型配置
     *
     * 为什么：按启用状态过滤模型配置
     * 入参：启用状态查询条件
     * 出参：模型配置列表
     */
    List<ModelConfig> findByEnabled(EnabledQuery query);

    /**
     * 查询所有启用的模型配置
     *
     * 为什么：获取全量可用模型配置
     * 入参：无
     * 出参：模型配置列表
     */
    List<ModelConfig> findByEnabledTrue();

    /**
     * 根据模型名称查询模型配置
     *
     * 为什么：名称用于唯一性校验与定位配置
     * 入参：模型名称查询条件
     * 出参：模型配置
     */
    Optional<ModelConfig> findByModelName(ModelNameQuery query);

    /**
     * 查询所有启用的模型配置并加载能力
     *
     * 为什么：需要同时展示能力信息
     * 入参：无
     * 出参：模型配置列表
     */
    List<ModelConfig> findByEnabledTrueWithCapability();

    /**
     * 根据ID列表查询启用的模型配置
     *
     * 为什么：按指定 ID 集合过滤可用模型
     * 入参：模型ID列表查询条件
     * 出参：模型配置列表
     */
    List<ModelConfig> findEnabledByIds(EnabledIdsQuery query);

    /**
     * 根据ID查询模型配置
     *
     * 为什么：按唯一 ID 获取模型配置
     * 入参：ID 查询条件
     * 出参：模型配置
     */
    Optional<ModelConfig> findById(IdQuery query);

    /**
     * 查询模型配置分页数据（包含能力）
     *
     * 为什么：分页展示需同时携带能力信息
     * 入参：分页查询条件
     * 出参：模型配置列表
     */
    List<ModelConfig> findPageWithCapability(ModelConfigPageQuery query);

    /**
     * 统计模型配置总数
     *
     * 为什么：分页展示需要总数
     * 入参：无
     * 出参：总数
     */
    long countAll();

    /**
     * 判断模型配置是否存在
     *
     * 为什么：更新/删除前校验存在性
     * 入参：ID 查询条件
     * 出参：是否存在
     */
    boolean existsById(IdQuery query);

    /**
     * 保存模型配置聚合（新增或更新）
     *
     * 为什么：保证模型与能力配置聚合一致性
     * 入参：模型配置聚合
     * 出参：保存后的聚合
     */
    ModelConfigAggregate save(ModelConfigAggregate aggregate);

    /**
     * 删除模型配置
     *
     * 为什么：移除无效配置
     * 入参：ID 查询条件
     * 出参：无
     */
    void deleteById(IdQuery query);
}
