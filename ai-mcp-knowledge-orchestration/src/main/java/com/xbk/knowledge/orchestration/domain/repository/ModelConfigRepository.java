package com.xbk.knowledge.orchestration.domain.repository;

import com.xbk.knowledge.orchestration.domain.entity.ModelConfig;
import com.xbk.knowledge.orchestration.model.enums.ModelType;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
