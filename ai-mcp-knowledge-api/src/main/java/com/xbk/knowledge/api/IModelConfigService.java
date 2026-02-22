package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.api.dto.model.ModelConfigQueryRequest;
import com.xbk.knowledge.api.dto.model.ModelConfigRequest;
import com.xbk.knowledge.api.dto.model.ModelConfigResponse;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;

/**
 * 模型配置服务接口
 * 定义 LLM 模型配置管理的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IModelConfigService {

    /**
     * 分页查询模型列表。
     *
     * @param request 请求参数
     * @return 列表结果
     */
    Result<PageResult<ModelConfigResponse>> listModels(ModelConfigQueryRequest request);

    /**
     * 查询模型详情。
     *
     * @param request 请求参数
     * @return 查询结果
     */
    Result<ModelConfigResponse> getModel(IdRequest request);

    /**
     * 创建模型配置。
     *
     * @param request 请求参数
     * @return 创建结果
     */
    Result<ModelConfigResponse> createModel(ModelConfigRequest request);

    /**
     * 更新模型配置。
     *
     * @param request 请求参数
     * @return 更新结果
     */
    Result<ModelConfigResponse> updateModel(ModelConfigRequest request);

    /**
     * 删除模型配置。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<Void> deleteModel(IdRequest request);

    /**
     * 启用模型。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<ModelConfigResponse> enableModel(IdRequest request);

    /**
     * 禁用模型。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<ModelConfigResponse> disableModel(IdRequest request);

    /**
     * 查询当前生效的对话模型。
     *
     * @return 查询结果
     */
    Result<ModelConfigResponse> getActiveChatModel();

    /**
     * 查询当前生效的向量模型。
     *
     * @return 查询结果
     */
    Result<ModelConfigResponse> getActiveEmbeddingModel();

    /**
     * 激活对话模型。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<ModelConfigResponse> activateChatModel(IdRequest request);

    /**
     * 激活向量模型。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<ModelConfigResponse> activateEmbeddingModel(IdRequest request);

    /**
     * 测试模型连通性。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<Boolean> testModel(IdRequest request);
}
