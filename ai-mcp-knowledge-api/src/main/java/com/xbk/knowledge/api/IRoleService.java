package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.role.RoleCreateRequest;
import com.xbk.knowledge.api.dto.role.RolePermissionGrantRequest;
import com.xbk.knowledge.api.dto.role.RolePermissionQueryRequest;
import com.xbk.knowledge.api.dto.role.RoleQueryRequest;
import com.xbk.knowledge.api.dto.role.RoleResponse;
import com.xbk.knowledge.api.dto.role.RoleUpdateRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;

import java.util.List;

/**
 * 角色服务接口
 * 定义角色与授权管理的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IRoleService {

    /**
     * 分页查询数据列表。
     *
     * @param request 请求参数
     * @return 列表结果
     */
    Result<PageResult<RoleResponse>> list(RoleQueryRequest request);

    /**
     * 创建数据。
     *
     * @param request 请求参数
     * @return 创建结果
     */
    Result<RoleResponse> create(RoleCreateRequest request);

    /**
     * 更新数据。
     *
     * @param request 请求参数
     * @return 更新结果
     */
    Result<RoleResponse> update(RoleUpdateRequest request);

    /**
     * 授予角色权限。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<Void> grantPermissions(RolePermissionGrantRequest request);

    /**
     * 查询角色已分配权限 ID 列表。
     *
     * @param request 请求参数
     * @return ID 列表结果
     */
    Result<List<Long>> permissionIds(RolePermissionQueryRequest request);
}
