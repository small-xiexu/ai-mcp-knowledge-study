package com.xbk.knowledge.domain.service.model;

import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.vo.common.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.model.ModelConfigPageQuery;
import com.xbk.knowledge.types.common.PageResult;

import java.util.List;

/**
 * 模型配置领域服务接口
 * 负责模型配置的业务逻辑处理
 *
 * 职责：领域服务接口，用于定义业务能力
 * @author xiexu
 */
public interface IModelConfigService {

    /**
     * 分页查询模型配置
     *
     * 为什么：统一分页查询能力入口
     * 入参：分页查询条件
     * 出参：分页结果
     */
    PageResult<ModelConfig> queryModelConfigPage(ModelConfigPageQuery query);

    /**
     * 根据 ID 查询模型配置
     *
     * 为什么：按唯一 ID 获取模型配置
     * 入参：ID 查询条件
     * 出参：模型配置
     */
    ModelConfig queryModelConfigById(IdQuery query);

    /**
     * 创建模型配置
     *
     * 为什么：统一创建入口以保障规则一致
     * 入参：模型配置实体
     * 出参：创建后的模型配置
     */
    ModelConfig createModelConfig(ModelConfig modelConfig);

    /**
     * 更新模型配置
     *
     * 为什么：统一更新入口以保障规则一致
     * 入参：模型配置实体（必须包含 ID）
     * 出参：更新后的模型配置
     */
    ModelConfig updateModelConfig(ModelConfig modelConfig);

    /**
     * 删除模型配置
     *
     * 为什么：统一删除入口以保障规则一致
     * 入参：ID 查询条件
     * 出参：无
     */
    void deleteModelConfig(IdQuery query);

    /**
     * 启用模型
     *
     * 为什么：统一启用入口以保障规则一致
     * 入参：ID 查询条件
     * 出参：更新后的模型配置
     */
    ModelConfig enableModel(IdQuery query);

    /**
     * 禁用模型
     *
     * 为什么：统一禁用入口以保障规则一致
     * 入参：ID 查询条件
     * 出参：更新后的模型配置
     */
    ModelConfig disableModel(IdQuery query);

    /**
     * 查询所有启用的模型
     *
     * 为什么：为路由/推荐提供数据源
     * 入参：启用状态查询条件
     * 出参：启用的模型列表
     */
    List<ModelConfig> queryEnabledModels(EnabledQuery query);

}
