package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.agent.PromptTemplateCreateRequest;
import com.xbk.knowledge.api.dto.agent.PromptTemplatePublishRequest;
import com.xbk.knowledge.api.dto.agent.PromptTemplateQueryRequest;
import com.xbk.knowledge.api.dto.agent.PromptTemplateResponse;
import com.xbk.knowledge.api.dto.agent.PromptTemplateUpdateRequest;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;

/**
 * 提示词模板服务接口。
 *
 * 职责：定义 Prompt 模板管理的 API 契约。
 *
 * @author sxie
 */
public interface IPromptTemplateService {

    /**
     * 按筛选条件分页查询提示词模板数据。
     *
     * @param request 提示词模板分页查询参数
     * @return 提示词模板分页数据
     */
    Result<PageResult<PromptTemplateResponse>> list(PromptTemplateQueryRequest request);

    /**
     * 查询提示词模板详情。
     *
     * @param request 提示词模板查询参数
     * @return 提示词模板详情
     */
    Result<PromptTemplateResponse> get(IdRequest request);

    /**
     * 创建提示词模板数据。
     *
     * @param request 提示词模板创建参数
     * @return 创建后的提示词模板信息
     */
    Result<PromptTemplateResponse> create(PromptTemplateCreateRequest request);

    /**
     * 更新提示词模板数据。
     *
     * @param request 提示词模板更新参数
     * @return 更新后的提示词模板信息
     */
    Result<PromptTemplateResponse> update(PromptTemplateUpdateRequest request);

    /**
     * 发布版本。
     *
     * @param request 提示词模板发布参数
     * @return 发布后的提示词模板信息
     */
    Result<PromptTemplateResponse> publish(PromptTemplatePublishRequest request);

    /**
     * 归档模板。
     *
     * @param request 提示词模板归档参数
     * @return 归档后的提示词模板信息
     */
    Result<PromptTemplateResponse> archive(PromptTemplatePublishRequest request);
}
