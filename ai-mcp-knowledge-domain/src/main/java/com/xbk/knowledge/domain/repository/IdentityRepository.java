package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.entity.SysUser;
import com.xbk.knowledge.domain.model.entity.SysRole;
import com.xbk.knowledge.domain.model.entity.SysPermission;
import com.xbk.knowledge.domain.model.vo.identity.UserPageQuery;
import com.xbk.knowledge.domain.model.vo.identity.RolePageQuery;
import com.xbk.knowledge.domain.model.vo.identity.PermissionPageQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 身份域仓储接口。
 *
 * 职责：身份与权限数据访问抽象。
 *
 * @author xiexu
 */
public interface IdentityRepository {

    /**
     * 按租户与用户名查询用户。
     *
     * @param tenantId 租户ID
     * @param username 用户名
     * @return 用户
     */
    Optional<SysUser> findByTenantAndUsername(String tenantId, String username);

    /**
     * 按用户ID查询用户。
     *
     * @param userId 用户ID
     * @return 用户
     */
    Optional<SysUser> findById(Long userId);

    /**
     * 查询用户的角色编码列表。
     *
     * @param tenantId 租户ID
     * @param userId   用户ID
     * @return 角色编码列表
     */
    List<String> findRoleCodes(String tenantId, Long userId);

    /**
     * 查询用户的权限编码列表。
     *
     * @param tenantId 租户ID
     * @param userId   用户ID
     * @return 权限编码列表
     */
    List<String> findPermissionCodes(String tenantId, Long userId);

    /**
     * 查询所有启用权限编码。
     *
     * @return 权限编码列表
     */
    List<String> findAllPermissionCodes();

    /**
     * 分页查询用户。
     *
     * @param query 查询条件
     * @return 用户列表
     */
    List<SysUser> findPage(UserPageQuery query);

    /**
     * 统计用户数量。
     *
     * @param query 查询条件
     * @return 总数
     */
    long count(UserPageQuery query);

    /**
     * 新增用户。
     *
     * @param user 用户实体
     * @return 新增后的用户
     */
    SysUser insert(SysUser user);

    /**
     * 判断租户内用户名是否已存在。
     *
     * @param tenantId 租户ID
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByTenantAndUsername(String tenantId, String username);

    /**
     * 更新最后登录信息。
     *
     * @param userId       用户ID
     * @param loginIp      登录IP
     * @param loginTime    登录时间
     * @return 影响行数
     */
    int updateLastLogin(Long userId, String loginIp, LocalDateTime loginTime);

    /**
     * 分页查询角色。
     *
     * @param query 查询条件
     * @return 角色列表
     */
    List<SysRole> findRolePage(RolePageQuery query);

    /**
     * 统计角色数量。
     *
     * @param query 查询条件
     * @return 总数
     */
    long countRole(RolePageQuery query);

    /**
     * 新增角色。
     *
     * @param role 角色实体
     * @return 新增后的角色
     */
    SysRole insertRole(SysRole role);

    /**
     * 更新角色。
     *
     * @param role 角色实体
     * @return 影响行数
     */
    int updateRole(SysRole role);

    /**
     * 按ID查询角色。
     *
     * @param roleId 角色ID
     * @return 角色
     */
    Optional<SysRole> findRoleById(Long roleId);

    /**
     * 判断租户内角色编码是否存在。
     *
     * @param tenantId 租户ID
     * @param roleCode 角色编码
     * @param excludeRoleId 排除的角色ID
     * @return 是否存在
     */
    boolean existsRoleCode(String tenantId, String roleCode, Long excludeRoleId);

    /**
     * 查询角色绑定的权限ID列表。
     *
     * @param tenantId 租户ID
     * @param roleId 角色ID
     * @return 权限ID列表
     */
    List<Long> findRolePermissionIds(String tenantId, Long roleId);

    /**
     * 重建角色权限绑定。
     *
     * @param tenantId 租户ID
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @param grantedBy 授权人
     */
    void replaceRolePermissions(String tenantId, Long roleId, List<Long> permissionIds, Long grantedBy);

    /**
     * 分页查询权限。
     *
     * @param query 查询条件
     * @return 权限列表
     */
    List<SysPermission> findPermissionPage(PermissionPageQuery query);

    /**
     * 统计权限数量。
     *
     * @param query 查询条件
     * @return 总数
     */
    long countPermission(PermissionPageQuery query);

    /**
     * 校验权限ID集合是否全部存在。
     *
     * @param permissionIds 权限ID集合
     * @return 命中数量
     */
    long countPermissionByIds(List<Long> permissionIds);

    /**
     * 查询用户角色ID列表。
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<Long> findUserRoleIds(String tenantId, Long userId);

    /**
     * 校验角色ID集合在租户内是否全部存在。
     *
     * @param tenantId 租户ID
     * @param roleIds 角色ID集合
     * @return 命中数量
     */
    long countRolesByIds(String tenantId, List<Long> roleIds);

    /**
     * 重建用户角色绑定。
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param roleIds 角色ID集合
     * @param grantedBy 授权人
     */
    void replaceUserRoles(String tenantId, Long userId, List<Long> roleIds, Long grantedBy);
}
