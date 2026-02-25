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
 * 用户身份服务接口。
 *
 * 职责：定义用户与角色关系管理的 API 契约。
 *
 * @author sxie
 */
public interface IUserIdentityService {

    /**
     * 按筛选条件分页查询用户身份数据。
     *
     * @param request 用户身份分页查询参数
     * @return 用户身份分页数据
     */
    Result<PageResult<UserResponse>> list(UserQueryRequest request);

    /**
     * 创建用户身份数据。
     *
     * @param request 用户身份创建参数
     * @return 创建后的用户信息
     */
    Result<UserResponse> create(UserCreateRequest request);

    /**
     * 更新用户身份数据。
     *
     * @param request 用户身份更新参数
     * @return 更新后的用户信息
     */
    Result<UserResponse> update(UserUpdateRequest request);

    /**
     * 重置用户密码。
     *
     * @param request 用户密码重置参数
     * @return 重置结果
     */
    Result<Void> resetPassword(UserPasswordResetRequest request);

    /**
     * 授予用户角色。
     *
     * @param request 用户角色授予参数
     * @return 授权结果
     */
    Result<Void> grantRoles(UserRoleGrantRequest request);

    /**
     * 查询用户已分配角色 ID 列表。
     *
     * @param request 用户角色查询参数
     * @return 已分配角色 ID 列表
     */
    Result<List<Long>> roleIds(UserRoleQueryRequest request);
}
