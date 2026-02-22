package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.client.ClientProfileQueryRequest;
import com.xbk.knowledge.api.dto.client.ClientProfileResponse;
import com.xbk.knowledge.api.dto.client.ClientProfileSaveRequest;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;

/**
 * 客户端画像服务接口
 * 定义客户端画像管理的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IClientProfileService {

    /**
     * 分页查询数据列表。
     *
     * @param request 请求参数
     * @return 列表结果
     */
    Result<PageResult<ClientProfileResponse>> list(ClientProfileQueryRequest request);

    /**
     * 查询详情信息。
     *
     * @param request 请求参数
     * @return 查询结果
     */
    Result<ClientProfileResponse> get(IdRequest request);

    /**
     * 保存配置信息。
     *
     * @param request 请求参数
     * @return 保存结果
     */
    Result<ClientProfileResponse> save(ClientProfileSaveRequest request);

    /**
     * 启用目标对象。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<ClientProfileResponse> enable(IdRequest request);

    /**
     * 禁用目标对象。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<ClientProfileResponse> disable(IdRequest request);

    /**
     * 删除目标对象。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<Void> remove(IdRequest request);
}
