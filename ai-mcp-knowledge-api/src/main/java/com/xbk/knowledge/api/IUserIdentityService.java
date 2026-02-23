package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.user.UserCreateRequest;
import com.xbk.knowledge.api.dto.user.UserPasswordResetRequest;
import com.xbk.knowledge.api.dto.user.UserQueryRequest;
import com.xbk.knowledge.api.dto.user.UserResponse;
import com.xbk.knowledge.api.dto.user.UserRoleGrantRequest;
import com.xbk.knowledge.api.dto.user.UserRoleQueryRequest;
import com.xbk.knowledge.api.dto.user.UserUpdateRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;

import java.util.List;

/**
 * 用户身份服务接口
 * 定义用户与角色关系管理的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IUserIdentityService {

    /**
     * 按筛选条件分页查询用户身份数据。
     *
     * @param request 用户身份分页查询参数。
     * @return 返回 UserResponse 分页数据。
     */
    Result<PageResult<UserResponse>> list(UserQueryRequest request);

    /**
     * 创建用户身份数据。
     *
     * @param request 用户身份创建参数。
     * @return 返回 UserResponse 数据。
     */
    Result<UserResponse> create(UserCreateRequest request);

    /**
     * 更新用户身份数据。
     *
     * @param request 用户身份更新参数。
     * @return 返回 UserResponse 数据。
     */
    Result<UserResponse> update(UserUpdateRequest request);

    /**
     * 重置用户密码。
     *
     * @param request 用户密码重置参数。
     * @return 返回用户密码重置状态。
     */
    Result<Void> resetPassword(UserPasswordResetRequest request);

    /**
     * 授予用户角色。
     *
     * @param request 用户角色授予参数。
     * @return 返回用户角色授予状态。
     */
    Result<Void> grantRoles(UserRoleGrantRequest request);

    /**
     * 查询用户已分配角色 ID 列表。
     *
     * @param request 用户角色查询参数。
     * @return 返回 Long 列表数据。
     */
    Result<List<Long>> roleIds(UserRoleQueryRequest request);
}
