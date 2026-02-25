package com.xbk.knowledge.domain.llm.service;

import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.llm.model.valobj.ModelConfigPageQuery;
import com.xbk.knowledge.types.common.PageResult;

import java.util.List;

/**
 * 模型配置领域服务接口
 * 负责模型配置的业务逻辑处理
 *
 * 职责：领域服务接口，用于定义业务能力
 * @author sxie
 */
public interface IModelConfigService {

    /**
     * 分页查询模型配置
     *
     * 统一分页查询能力入口
     * 
     * @param query 分页查询条件。
     * @return 模型配置分页结果。
     */
    PageResult<ModelConfig> queryModelConfigPage(ModelConfigPageQuery query);

    /**
     * 根据 ID 查询模型配置
     *
     * 按唯一 ID 获取模型配置
     * 
     * @param query 主键查询条件。
     * @return 模型配置详情。
     */
    ModelConfig queryModelConfigById(IdQuery query);

    /**
     * 创建模型配置
     *
     * 统一创建入口以保障规则一致
     * 
     * @param modelConfig 模型配置。
     * @return 创建后的模型配置。
     */
    ModelConfig createModelConfig(ModelConfig modelConfig);

    /**
     * 更新模型配置
     *
     * 统一更新入口以保障规则一致
     * 
     * @param modelConfig 模型配置。
     * @return 更新后的模型配置。
     */
    ModelConfig updateModelConfig(ModelConfig modelConfig);

    /**
     * 删除模型配置
     *
     * 统一删除入口以保障规则一致
     * 
     * @param query 主键查询条件。
     */
    void deleteModelConfig(IdQuery query);

    /**
     * 启用模型
     *
     * 统一启用入口以保障规则一致
     * 
     * @param query 主键查询条件。
     * @return 启用后的模型配置。
     */
    ModelConfig enableModel(IdQuery query);

    /**
     * 禁用模型
     *
     * 统一禁用入口以保障规则一致
     * 
     * @param query 主键查询条件。
     * @return 禁用后的模型配置。
     */
    ModelConfig disableModel(IdQuery query);

    /**
     * 查询所有启用的模型
     *
     * 为路由/推荐提供数据源
     * 
     * @param query 启用状态查询条件。
     * @return 模型配置列表。
     */
    List<ModelConfig> queryEnabledModels(EnabledQuery query);

}
