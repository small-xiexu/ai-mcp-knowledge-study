package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.vo.EnabledIdsQuery;
import com.xbk.knowledge.domain.model.vo.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.IdQuery;
import com.xbk.knowledge.domain.model.vo.ModelConfigPageQuery;
import com.xbk.knowledge.domain.model.vo.ModelNameQuery;
import com.xbk.knowledge.domain.model.vo.ModelTypeEnabledQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 模型配置 Mapper
 * 使用 XML 执行 SQL，避免注解或默认方法绕过约束
 *
 * 职责：MyBatis Mapper 接口，用于映射数据库操作
 * @author xiexu
 */
@Mapper
public interface ModelConfigMapper extends BaseMapper<ModelConfig> {

    /**
     * 新增模型配置
     *
     * @param modelConfig 模型配置
     * @return 影响行数
     */
    int insertModelConfig(ModelConfig modelConfig);

    /**
     * 更新模型配置
     *
     * @param modelConfig 模型配置
     * @return 影响行数
     */
    int updateModelConfig(ModelConfig modelConfig);

    /**
     * 删除模型配置
     *
     * @param query ID 查询条件
     * @return 影响行数
     */
    int deleteModelConfigById(IdQuery query);

    /**
     * 根据ID查询模型配置
     *
     * @param query ID 查询条件
     * @return 模型配置
     */
    ModelConfig findById(IdQuery query);

    /**
     * 根据ID查询模型配置（包含能力）
     *
     * @param query ID 查询条件
     * @return 模型配置
     */
    ModelConfig findByIdWithCapability(IdQuery query);

    /**
     * 根据模型名称查询模型配置
     *
     * @param query 模型名称查询条件
     * @return 模型配置
     */
    ModelConfig findByModelName(ModelNameQuery query);

    /**
     * 根据模型类型和启用状态查询模型配置
     *
     * @param query 模型类型与启用状态查询条件
     * @return 模型配置列表
     */
    List<ModelConfig> findByModelTypeAndEnabled(ModelTypeEnabledQuery query);

    /**
     * 根据启用状态查询模型配置
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
    List<ModelConfig> findEnabledTrue();

    /**
     * 查询所有启用的模型配置，按优先级降序排序
     *
     * @param query 启用状态查询条件
     * @return 模型配置列表
     */
    List<ModelConfig> findByEnabledOrderByPriorityDesc(EnabledQuery query);

    /**
     * 查询所有启用的模型配置并加载能力
     *
     * @return 模型配置列表
     */
    List<ModelConfig> findEnabledTrueWithCapability();

    /**
     * 根据ID列表查询启用的模型配置
     *
     * @param query 模型ID列表查询条件
     * @return 模型配置列表
     */
    List<ModelConfig> findEnabledByIds(EnabledIdsQuery query);

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
}
