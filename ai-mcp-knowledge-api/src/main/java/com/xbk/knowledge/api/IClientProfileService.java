package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.client.ClientProfileQueryRequest;
import com.xbk.knowledge.api.dto.client.ClientProfileResponse;
import com.xbk.knowledge.api.dto.client.ClientProfileSaveRequest;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;

/**
 * 客户端画像服务接口。
 *
 * 职责：定义客户端画像管理的 API 契约。
 *
 * @author sxie
 */
public interface IClientProfileService {

    /**
     * 按筛选条件分页查询客户端画像数据。
     *
     * @param request 客户端画像分页查询参数
     * @return 客户端画像分页数据
     */
    Result<PageResult<ClientProfileResponse>> list(ClientProfileQueryRequest request);

    /**
     * 查询客户端画像详情。
     *
     * @param request 客户端画像查询参数
     * @return 客户端画像详情
     */
    Result<ClientProfileResponse> get(IdRequest request);

    /**
     * 创建或更新客户端画像。
     *
     * @param request 客户端画像保存参数
     * @return 保存后的客户端画像信息
     */
    Result<ClientProfileResponse> save(ClientProfileSaveRequest request);

    /**
     * 启用客户端画像。
     *
     * @param request 客户端画像启用参数
     * @return 启用后的客户端画像信息
     */
    Result<ClientProfileResponse> enable(IdRequest request);

    /**
     * 禁用客户端画像。
     *
     * @param request 客户端画像禁用参数
     * @return 禁用后的客户端画像信息
     */
    Result<ClientProfileResponse> disable(IdRequest request);

    /**
     * 删除客户端画像。
     *
     * @param request 客户端画像删除参数
     * @return 删除结果
     */
    Result<Void> remove(IdRequest request);
}
