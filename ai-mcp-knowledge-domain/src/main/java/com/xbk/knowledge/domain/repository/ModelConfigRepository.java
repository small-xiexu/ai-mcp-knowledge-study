package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.vo.EnabledIdsQuery;
import com.xbk.knowledge.domain.model.vo.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.IdQuery;
import com.xbk.knowledge.domain.model.vo.ModelConfigPageQuery;
import com.xbk.knowledge.domain.model.vo.ModelNameQuery;
import com.xbk.knowledge.domain.model.vo.ModelTypeEnabledQuery;
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
     * 根据模型类型查询启用的模型配置
     *
     * @param query 模型类型与启用状态查询条件
     * @return 模型配置列表
     */
    List<ModelConfig> findByModelTypeAndEnabled(ModelTypeEnabledQuery query);

    /**
     * 查询启用模型配置并按优先级降序排序
     *
     * @param query 启用状态查询条件
     * @return 模型配置列表
     */
    List<ModelConfig> findByEnabledOrderByPriorityDesc(EnabledQuery query);

    /**
     * 查询指定启用状态的模型配置
     *
     * @param query 启用状态查询条件
     * @return 模型配置列表
     */
    List<ModelConfig> findByEnabled(EnabledQuery query);

    /**
     * 查询所有启用的模型配置
     *
     * @return 模型配置列表
     */
    List<ModelConfig> findByEnabledTrue();

    /**
     * 根据模型名称查询模型配置
     *
     * @param query 模型名称查询条件
     * @return 模型配置
     */
    Optional<ModelConfig> findByModelName(ModelNameQuery query);

    /**
     * 查询所有启用的模型配置并加载能力
     *
     * @return 模型配置列表
     */
    List<ModelConfig> findByEnabledTrueWithCapability();

    /**
     * 根据ID列表查询启用的模型配置
     *
     * @param query 模型ID列表查询条件
     * @return 模型配置列表
     */
    List<ModelConfig> findEnabledByIds(EnabledIdsQuery query);

    /**
     * 根据ID查询模型配置
     *
     * @param query ID 查询条件
     * @return 模型配置
     */
    Optional<ModelConfig> findById(IdQuery query);

    /**
     * 查询模型配置分页数据（包含能力）
     *
     * @param query 分页查询条件
     * @return 模型配置列表
     */
    List<ModelConfig> findPageWithCapability(ModelConfigPageQuery query);

    /**
     * 统计模型配置总数
     *
     * @return 总数
     */
    long countAll();

    /**
     * 判断模型配置是否存在
     *
     * @param query ID 查询条件
     * @return 是否存在
     */
    boolean existsById(IdQuery query);

    /**
     * 保存模型配置（新增或更新）
     *
     * @param modelConfig 模型配置
     * @return 保存后的模型配置
     */
    ModelConfig save(ModelConfig modelConfig);

    /**
     * 删除模型配置
     *
     * @param query ID 查询条件
     */
    void deleteById(IdQuery query);
}
