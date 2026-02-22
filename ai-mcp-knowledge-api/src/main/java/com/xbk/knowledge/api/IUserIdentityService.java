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
     * 分页查询数据列表。
     *
     * @param request 请求参数
     * @return 列表结果
     */
    Result<PageResult<UserResponse>> list(UserQueryRequest request);

    /**
     * 创建数据。
     *
     * @param request 请求参数
     * @return 创建结果
     */
    Result<UserResponse> create(UserCreateRequest request);

    /**
     * 更新数据。
     *
     * @param request 请求参数
     * @return 更新结果
     */
    Result<UserResponse> update(UserUpdateRequest request);

    /**
     * 重置用户密码。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<Void> resetPassword(UserPasswordResetRequest request);

    /**
     * 授予用户角色。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<Void> grantRoles(UserRoleGrantRequest request);

    /**
     * 查询用户已分配角色 ID 列表。
     *
     * @param request 请求参数
     * @return ID 列表结果
     */
    Result<List<Long>> roleIds(UserRoleQueryRequest request);
}
