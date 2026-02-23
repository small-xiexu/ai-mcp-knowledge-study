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
     * 按筛选条件分页查询角色数据。
     *
     * @param request 角色分页查询条件。
     * @return 返回 RoleResponse 分页数据。
     */
    Result<PageResult<RoleResponse>> list(RoleQueryRequest request);

    /**
     * 创建角色数据。
     *
     * @param request 角色创建参数。
     * @return 返回 RoleResponse 数据。
     */
    Result<RoleResponse> create(RoleCreateRequest request);

    /**
     * 更新角色数据。
     *
     * @param request 角色更新参数。
     * @return 返回 RoleResponse 数据。
     */
    Result<RoleResponse> update(RoleUpdateRequest request);

    /**
     * 授予角色权限。
     *
     * @param request 角色授权参数。
     * @return 返回角色授权保存状态。
     */
    Result<Void> grantPermissions(RolePermissionGrantRequest request);

    /**
     * 查询角色已分配权限 ID 列表。
     *
     * @param request 角色权限查询参数。
     * @return 返回 Long 列表数据。
     */
    Result<List<Long>> permissionIds(RolePermissionQueryRequest request);
}
