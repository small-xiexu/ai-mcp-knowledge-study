package com.xbk.knowledge.orchestration.domain.repository;

import com.xbk.knowledge.orchestration.domain.entity.ModelConfig;
import com.xbk.knowledge.orchestration.model.enums.ModelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 模型配置 Repository
 *
 * @author xiexu
 */
@Repository
public interface ModelConfigRepository extends JpaRepository<ModelConfig, Long> {

    /**
     * 根据模型类型查询所有启用的模型配置
     *
     * @param modelType 模型类型
     * @param enabled   是否启用
     * @return 模型配置列表
     */
    List<ModelConfig> findByModelTypeAndEnabled(ModelType modelType, Boolean enabled);

    /**
     * 查询所有启用的模型配置，按优先级降序排序
     *
     * @param enabled 是否启用
     * @return 模型配置列表
     */
    List<ModelConfig> findByEnabledOrderByPriorityDesc(Boolean enabled);

    /**
     * 查询所有启用的模型配置，按质量评分降序排序（通过关联查询）
     * 注意：需要在 Service 层通过 JOIN 查询实现
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
     * 根据模型名称查询
     *
     * @param modelName 模型名称
     * @return 模型配置
     */
    Optional<ModelConfig> findByModelName(String modelName);

    /**
     * 查询所有启用的模型配置，并通过 JOIN FETCH 加载关联的 capability
     * 避免 N+1 查询问题
     *
     * @return 模型配置列表（包含 capability）
     */
    @Query("SELECT m FROM ModelConfig m LEFT JOIN FETCH m.capability WHERE m.enabled = true")
    List<ModelConfig> findByEnabledTrueWithCapability();

    /**
     * 根据 ID 列表查询启用的模型配置
     * 在 SQL 中直接过滤启用状态，提高性能
     *
     * @param ids 模型 ID 列表
     * @return 启用的模型配置列表
     */
    @Query("SELECT m FROM ModelConfig m WHERE m.id IN :ids AND m.enabled = true")
    List<ModelConfig> findEnabledByIds(@Param("ids") List<Long> ids);
}
