package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.identity.model.entity.SysUser;
import com.xbk.knowledge.domain.identity.model.valobj.UserPageQuery;
import com.xbk.knowledge.types.common.PageResult;

import java.util.List;

/**
 * 用户管理应用服务接口。
 *
 * 职责：应用层用例接口，用于封装用户管理能力。
 *
 * @author sxie
 */
public interface UserIdentityAppService {

    /**
     * 分页查询用户。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<SysUser> queryUserPage(UserPageQuery query);

    /**
     * 创建用户。
     *
     * @param user        用户实体
     * @param rawPassword 明文密码
     * @return 创建后的用户
     */
    SysUser createUser(SysUser user, String rawPassword);

    /**
     * 更新用户基础信息。
     *
     * @param user 用户实体
     * @return 更新后的用户
     */
    SysUser updateUser(SysUser user);

    /**
     * 重置用户密码。
     *
     * @param userId 用户ID
     * @param rawPassword 新密码明文
     */
    void resetPassword(Long userId, String rawPassword);

    /**
     * 查询用户已绑定的角色 ID 列表。
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<Long> queryRoleIds(Long userId);

    /**
     * 重新绑定用户角色。
     *
     * @param userId 目标用户ID
     * @param roleIds 角色ID集合
     * @param operatorId 操作人ID
     */
    void grantRoles(Long userId, List<Long> roleIds, Long operatorId);
}
