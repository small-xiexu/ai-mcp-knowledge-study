package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.permission.PermissionQueryRequest;
import com.xbk.knowledge.api.dto.permission.PermissionResponse;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;

/**
 * 权限服务接口
 * 定义权限查询管理的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IPermissionService {

    /**
     * 分页查询数据列表。
     *
     * @param request 请求参数
     * @return 列表结果
     */
    Result<PageResult<PermissionResponse>> list(PermissionQueryRequest request);
}
