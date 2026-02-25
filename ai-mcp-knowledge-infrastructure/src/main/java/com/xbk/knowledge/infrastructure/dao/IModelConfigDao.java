package com.xbk.knowledge.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.infrastructure.dao.po.ModelConfigPO;
import com.xbk.knowledge.domain.common.model.valobj.EnabledIdsQuery;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.llm.model.valobj.ModelConfigPageQuery;
import com.xbk.knowledge.domain.llm.model.valobj.ModelNameQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 模型配置 Mapper
 * 使用 XML 执行 SQL，避免注解或默认方法绕过约束
 *
 * 职责：MyBatis Mapper 接口，用于映射数据库操作
 *
 * @author sxie
 */
@Mapper
public interface IModelConfigDao extends BaseMapper<ModelConfigPO> {

    /**
     * 新增模型配置
     *
     * 落库模型配置
     * 
     * @param modelConfig 模型配置。
     * @return 影响行数。
     */
    int insertModelConfig(ModelConfigPO modelConfig);

    /**
     * 更新模型配置
     *
     * 更新模型配置字段
     * 
     * @param modelConfig 模型配置。
     * @return 影响行数。
     */
    int updateModelConfig(ModelConfigPO modelConfig);

    /**
     * 删除模型配置
     *
     * 清理无效配置
     * 
     * @param query 主键查询条件。
     * @return 影响行数。
     */
    int deleteModelConfigById(IdQuery query);

    /**
     * 根据ID查询模型配置
     *
     * 按唯一 ID 获取配置
     * 
     * @param query 主键查询条件。
     * @return 模型配置持久化实体。
     */
    ModelConfigPO findById(IdQuery query);

    /**
     * 根据模型名称查询模型配置
     *
     * 用于唯一性校验
     * 
     * @param query 模型名称查询条件。
     * @return 模型配置持久化实体。
     */
    ModelConfigPO findByModelName(ModelNameQuery query);

    /**
     * 根据启用状态查询模型配置
     *
     * 按启用状态筛选
     * 
     * @param query 启用状态查询条件。
     * @return ModelConfigPO 列表。
     */
    List<ModelConfigPO> findByEnabled(EnabledQuery query);

    /**
     * 查询所有启用的模型配置
     *
     * 获取全量可用模型
     * 
     * @return ModelConfigPO 列表。
     */
    List<ModelConfigPO> findEnabledTrue();

    /**
     * 根据ID列表查询启用的模型配置
     *
     * 按指定 ID 集合筛选可用模型
     * 
     * @param query 启用模型 ID 集合查询条件。
     * @return ModelConfigPO 列表。
     */
    List<ModelConfigPO> findEnabledByIds(EnabledIdsQuery query);

    /**
     * 查询模型配置分页数据
     *
     * 分页展示模型配置
     * 
     * @param query 分页查询条件。
     * @return ModelConfigPO 列表。
     */
    List<ModelConfigPO> findPage(ModelConfigPageQuery query);

    /**
     * 统计模型配置总数
     *
     * 分页展示需要总数
     * 
     * @return 统计数量。
     */
    long countAll();
}
