package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.types.enums.ModelType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
     * @param id 模型ID
     * @return 影响行数
     */
    int deleteModelConfigById(@Param("id") Long id);

    /**
     * 根据ID查询模型配置
     *
     * @param id 模型ID
     * @return 模型配置
     */
    ModelConfig findById(@Param("id") Long id);

    /**
     * 根据ID查询模型配置（包含能力）
     *
     * @param id 模型ID
     * @return 模型配置
     */
    ModelConfig findByIdWithCapability(@Param("id") Long id);

    /**
     * 根据模型名称查询模型配置
     *
     * @param modelName 模型名称
     * @return 模型配置
     */
    ModelConfig findByModelName(@Param("modelName") String modelName);

    /**
     * 根据模型类型和启用状态查询模型配置
     *
     * @param modelType 模型类型
     * @param enabled   是否启用
     * @return 模型配置列表
     */
    List<ModelConfig> findByModelTypeAndEnabled(@Param("modelType") ModelType modelType,
                                                @Param("enabled") Boolean enabled);

    /**
     * 根据启用状态查询模型配置
     *
     * @param enabled 是否启用
     * @return 模型配置列表
     */
    List<ModelConfig> findByEnabled(@Param("enabled") Boolean enabled);

    /**
     * 查询所有启用的模型配置
     *
     * @return 模型配置列表
     */
    List<ModelConfig> findEnabledTrue();

    /**
     * 查询所有启用的模型配置，按优先级降序排序
     *
     * @param enabled 是否启用
     * @return 模型配置列表
     */
    List<ModelConfig> findByEnabledOrderByPriorityDesc(@Param("enabled") Boolean enabled);

    /**
     * 查询所有启用的模型配置并加载能力
     *
     * @return 模型配置列表
     */
    List<ModelConfig> findEnabledTrueWithCapability();

    /**
     * 根据ID列表查询启用的模型配置
     *
     * @param ids 模型ID列表
     * @return 模型配置列表
     */
    List<ModelConfig> findEnabledByIds(@Param("ids") List<Long> ids);

    /**
     * 查询模型配置分页数据（包含能力）
     *
     * @param offset   偏移量
     * @param pageSize 每页大小
     * @return 模型配置列表
     */
    List<ModelConfig> findPageWithCapability(@Param("offset") int offset,
                                             @Param("pageSize") int pageSize);

    /**
     * 统计模型配置总数
     *
     * @return 总数
     */
    long countAll();
}
