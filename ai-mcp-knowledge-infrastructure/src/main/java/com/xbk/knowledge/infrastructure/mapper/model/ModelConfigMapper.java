package com.xbk.knowledge.infrastructure.mapper.model;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.vo.common.EnabledIdsQuery;
import com.xbk.knowledge.domain.model.vo.common.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.model.ModelConfigPageQuery;
import com.xbk.knowledge.domain.model.vo.model.ModelNameQuery;
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
     * 为什么：落库模型配置
     * 入参：模型配置
     * 出参：影响行数
     */
    int insertModelConfig(ModelConfig modelConfig);

    /**
     * 更新模型配置
     *
     * 为什么：更新模型配置字段
     * 入参：模型配置
     * 出参：影响行数
     */
    int updateModelConfig(ModelConfig modelConfig);

    /**
     * 删除模型配置
     *
     * 为什么：清理无效配置
     * 入参：ID 查询条件
     * 出参：影响行数
     */
    int deleteModelConfigById(IdQuery query);

    /**
     * 根据ID查询模型配置
     *
     * 为什么：按唯一 ID 获取配置
     * 入参：ID 查询条件
     * 出参：模型配置
     */
    ModelConfig findById(IdQuery query);

    /**
     * 根据ID查询模型配置（包含能力）
     *
     * 为什么：详情展示需要能力信息
     * 入参：ID 查询条件
     * 出参：模型配置
     */
    ModelConfig findByIdWithCapability(IdQuery query);

    /**
     * 根据模型名称查询模型配置
     *
     * 为什么：用于唯一性校验
     * 入参：模型名称查询条件
     * 出参：模型配置
     */
    ModelConfig findByModelName(ModelNameQuery query);

    /**
     * 根据启用状态查询模型配置
     *
     * 为什么：按启用状态筛选
     * 入参：启用状态查询条件
     * 出参：模型配置列表
     */
    List<ModelConfig> findByEnabled(EnabledQuery query);

    /**
     * 查询所有启用的模型配置
     *
     * 为什么：获取全量可用模型
     * 入参：无
     * 出参：模型配置列表
     */
    List<ModelConfig> findEnabledTrue();

    /**
     * 查询所有启用的模型配置，按优先级降序排序
     *
     * 为什么：用于优先级排序
     * 入参：启用状态查询条件
     * 出参：模型配置列表
     */
    List<ModelConfig> findByEnabledOrderByPriorityDesc(EnabledQuery query);

    /**
     * 查询所有启用的模型配置并加载能力
     *
     * 为什么：提供能力信息
     * 入参：无
     * 出参：模型配置列表
     */
    List<ModelConfig> findEnabledTrueWithCapability();

    /**
     * 根据ID列表查询启用的模型配置
     *
     * 为什么：按指定 ID 集合筛选可用模型
     * 入参：模型ID列表查询条件
     * 出参：模型配置列表
     */
    List<ModelConfig> findEnabledByIds(EnabledIdsQuery query);

    /**
     * 查询模型配置分页数据（包含能力）
     *
     * 为什么：分页展示需要能力信息
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
}
