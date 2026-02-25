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
     * 按筛选条件分页查询权限数据。
     * 
     * @param request 权限分页查询参数。
     * @return PermissionResponse 分页数据。
     */
    Result<PageResult<PermissionResponse>> list(PermissionQueryRequest request);
}
