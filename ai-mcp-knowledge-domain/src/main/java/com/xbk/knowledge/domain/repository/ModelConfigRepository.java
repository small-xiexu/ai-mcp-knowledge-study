package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.types.enums.ModelType;

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
     * @param modelType 模型类型
     * @param enabled   是否启用
     * @return 模型配置列表
     */
    List<ModelConfig> findByModelTypeAndEnabled(ModelType modelType, Boolean enabled);

    /**
     * 查询启用模型配置并按优先级降序排序
     *
     * @param enabled 是否启用
     * @return 模型配置列表
     */
    List<ModelConfig> findByEnabledOrderByPriorityDesc(Boolean enabled);

    /**
     * 查询指定启用状态的模型配置
     *
     * @param enabled 是否启用
     * @return 模型配置列表
     */
    List<ModelConfig> findByEnabled(Boolean enabled);

    /**
     * 查询所有启用的模型配置
     *
     * @return 模型配置列表
     */
    List<ModelConfig> findByEnabledTrue();

    /**
     * 根据模型名称查询模型配置
     *
     * @param modelName 模型名称
     * @return 模型配置
     */
    Optional<ModelConfig> findByModelName(String modelName);

    /**
     * 查询所有启用的模型配置并加载能力
     *
     * @return 模型配置列表
     */
    List<ModelConfig> findByEnabledTrueWithCapability();

    /**
     * 根据ID列表查询启用的模型配置
     *
     * @param ids 模型ID列表
     * @return 模型配置列表
     */
    List<ModelConfig> findEnabledByIds(List<Long> ids);

    /**
     * 根据ID查询模型配置
     *
     * @param id 模型ID
     * @return 模型配置
     */
    Optional<ModelConfig> findById(Long id);

    /**
     * 查询模型配置分页数据（包含能力）
     *
     * @param offset   偏移量
     * @param pageSize 每页大小
     * @return 模型配置列表
     */
    List<ModelConfig> findPageWithCapability(int offset, int pageSize);

    /**
     * 统计模型配置总数
     *
     * @return 总数
     */
    long countAll();

    /**
     * 判断模型配置是否存在
     *
     * @param id 模型ID
     * @return 是否存在
     */
    boolean existsById(Long id);

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
     * @param id 模型ID
     */
    void deleteById(Long id);
}
