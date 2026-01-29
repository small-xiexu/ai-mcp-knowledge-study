package com.xbk.knowledge.domain.service;

import com.xbk.knowledge.domain.model.entity.ModelConfig;
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
     * @param offset   偏移量
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageResult<ModelConfig> queryModelConfigPage(int offset, int pageSize);

    /**
     * 根据 ID 查询模型配置
     *
     * @param id 模型 ID
     * @return 模型配置
     */
    ModelConfig queryModelConfigById(Long id);

    /**
     * 创建模型配置
     *
     * @param modelConfig 模型配置实体
     * @return 创建后的模型配置
     */
    ModelConfig createModelConfig(ModelConfig modelConfig);

    /**
     * 更新模型配置
     *
     * @param modelConfig 模型配置实体（必须包含 ID）
     * @return 更新后的模型配置
     */
    ModelConfig updateModelConfig(ModelConfig modelConfig);

    /**
     * 删除模型配置
     *
     * @param id 模型 ID
     */
    void deleteModelConfig(Long id);

    /**
     * 启用模型
     *
     * @param id 模型 ID
     * @return 更新后的模型配置
     */
    ModelConfig enableModel(Long id);

    /**
     * 禁用模型
     *
     * @param id 模型 ID
     * @return 更新后的模型配置
     */
    ModelConfig disableModel(Long id);

    /**
     * 查询所有启用的模型
     *
     * @return 启用的模型列表
     */
    List<ModelConfig> queryEnabledModels();

    /**
     * 根据任务类型获取推荐模型
     *
     * @param taskType 任务类型
     * @return 推荐的模型配置
     */
    ModelConfig getRecommendedModel(String taskType);
}
