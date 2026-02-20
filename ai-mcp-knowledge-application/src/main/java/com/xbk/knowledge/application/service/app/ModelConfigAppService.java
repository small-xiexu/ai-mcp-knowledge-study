package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.llm.model.valobj.ModelConfigPageQuery;
import com.xbk.knowledge.types.common.PageResult;

import java.util.List;

/**
 * 模型配置应用服务接口
 * 负责模型配置相关用例编排
 *
 * 职责：应用层用例接口，用于封装调用入口
 * @author sxie
 */
public interface ModelConfigAppService {

    /**
     * 分页查询模型配置
     *
     * @param query 分页查询条件
     * @return 分页结果
     */
    PageResult<ModelConfig> queryModelConfigPage(ModelConfigPageQuery query);

    /**
     * 根据 ID 查询模型配置
     *
     * @param query ID 查询条件
     * @return 模型配置
     */
    ModelConfig queryModelConfigById(IdQuery query);

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
     * @param query ID 查询条件
     */
    void deleteModelConfig(IdQuery query);

    /**
     * 启用模型
     *
     * @param query ID 查询条件
     * @return 更新后的模型配置
     */
    ModelConfig enableModel(IdQuery query);

    /**
     * 禁用模型
     *
     * @param query ID 查询条件
     * @return 更新后的模型配置
     */
    ModelConfig disableModel(IdQuery query);

    /**
     * 查询所有启用的模型
     *
     * @param query 启用状态查询条件
     * @return 启用的模型列表
     */
    List<ModelConfig> queryEnabledModels(EnabledQuery query);

    /**
     * 获取当前激活的对话模型
     *
     * @return 对话模型配置
     */
    ModelConfig getActiveChatModel();

    /**
     * 获取当前激活的嵌入模型
     *
     * @return 嵌入模型配置
     */
    ModelConfig getActiveEmbeddingModel();

    /**
     * 激活对话模型
     *
     * @param query 模型ID
     * @return 激活后的模型配置
     */
    ModelConfig activateChatModel(IdQuery query);

    /**
     * 激活嵌入模型
     *
     * @param query 模型ID
     * @return 激活后的模型配置
     */
    ModelConfig activateEmbeddingModel(IdQuery query);

    /**
     * 测试模型配置连接
     *
     * @param modelConfig 模型配置
     * @return 是否可用
     */
    boolean testModelConnection(ModelConfig modelConfig);
}
