package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.SysPermission;
import com.xbk.knowledge.domain.model.entity.SysRole;
import com.xbk.knowledge.domain.model.entity.SysUser;
import com.xbk.knowledge.domain.model.vo.identity.PermissionPageQuery;
import com.xbk.knowledge.domain.model.vo.identity.RolePageQuery;
import com.xbk.knowledge.domain.model.vo.identity.UserPageQuery;
import com.xbk.knowledge.domain.repository.IdentityRepository;
import com.xbk.knowledge.infrastructure.mapper.IdentityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 身份域仓储实现。
 *
 * 职责：基础设施层实现，用于落地身份与权限数据访问。
 *
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class IdentityRepositoryImpl implements IdentityRepository {

    private final IdentityMapper identityMapper;

    /**
     * 按租户与用户名查询用户。
     *
     * @param tenantId 租户ID
     * @param username 用户名
     * @return 用户
     */
    @Override
    public Optional<SysUser> findByTenantAndUsername(String tenantId, String username) {
        SysUser user = identityMapper.findByTenantAndUsername(tenantId, username);
        return Optional.ofNullable(user);
    }

    /**
     * 按用户ID查询用户。
     *
     * @param userId 用户ID
     * @return 用户
     */
    @Override
    public Optional<SysUser> findById(Long userId) {
        SysUser user = identityMapper.findById(userId);
        return Optional.ofNullable(user);
    }

    /**
     * 查询用户的角色编码列表。
     *
     * @param tenantId 租户ID
     * @param userId   用户ID
     * @return 角色编码列表
     */
    @Override
    public List<String> findRoleCodes(String tenantId, Long userId) {
        return identityMapper.findRoleCodes(tenantId, userId);
    }

    /**
     * 查询用户的权限编码列表。
     *
     * @param tenantId 租户ID
     * @param userId   用户ID
     * @return 权限编码列表
     */
    @Override
    public List<String> findPermissionCodes(String tenantId, Long userId) {
        return identityMapper.findPermissionCodes(tenantId, userId);
    }

    /**
     * 查询所有启用权限编码。
     *
     * @return 权限编码列表
     */
    @Override
    public List<String> findAllPermissionCodes() {
        return identityMapper.findAllPermissionCodes();
    }

    /**
     * 分页查询用户。
     *
     * @param query 查询条件
     * @return 用户列表
     */
    @Override
    public List<SysUser> findPage(UserPageQuery query) {
        return identityMapper.findPage(query);
    }

    /**
     * 统计用户数量。
     *
     * @param query 查询条件
     * @return 总数
     */
    @Override
    public long count(UserPageQuery query) {
        return identityMapper.count(query);
    }

    /**
     * 新增用户。
     *
     * @param user 用户实体
     * @return 新增后的用户
     */
    @Override
    public SysUser insert(SysUser user) {
        identityMapper.insertUser(user);
        return user;
    }

    /**
     * 判断租户内用户名是否已存在。
     *
     * @param tenantId 租户ID
     * @param username 用户名
     * @return 是否存在
     */
    @Override
    public boolean existsByTenantAndUsername(String tenantId, String username) {
        return identityMapper.countByTenantAndUsername(tenantId, username) > 0;
    }

    /**
     * 更新最后登录信息。
     *
     * @param userId    用户ID
     * @param loginIp   登录IP
     * @param loginTime 登录时间
     * @return 影响行数
     */
    @Override
    public int updateLastLogin(Long userId, String loginIp, LocalDateTime loginTime) {
        return identityMapper.updateLastLogin(userId, loginIp, loginTime);
    }

    /**
     * 分页查询角色。
     *
     * @param query 查询条件
     * @return 角色列表
     */
    @Override
    public List<SysRole> findRolePage(RolePageQuery query) {
        return identityMapper.findRolePage(query);
    }

    /**
     * 统计角色数量。
     *
     * @param query 查询条件
     * @return 总数
     */
    @Override
    public long countRole(RolePageQuery query) {
        return identityMapper.countRole(query);
    }

    /**
     * 新增角色。
     *
     * @param role 角色实体
     * @return 新增后的角色
     */
    @Override
    public SysRole insertRole(SysRole role) {
        identityMapper.insertRole(role);
        return role;
    }

    /**
     * 更新角色。
     *
     * @param role 角色实体
     * @return 影响行数
     */
    @Override
    public int updateRole(SysRole role) {
        return identityMapper.updateRole(role);
    }

    /**
     * 按ID查询角色。
     *
     * @param roleId 角色ID
     * @return 角色
     */
    @Override
    public Optional<SysRole> findRoleById(Long roleId) {
        return Optional.ofNullable(identityMapper.findRoleById(roleId));
    }

    /**
     * 判断租户内角色编码是否存在。
     *
     * @param tenantId 租户ID
     * @param roleCode 角色编码
     * @param excludeRoleId 排除的角色ID
     * @return 是否存在
     */
    @Override
    public boolean existsRoleCode(String tenantId, String roleCode, Long excludeRoleId) {
        return identityMapper.countByTenantAndRoleCode(tenantId, roleCode, excludeRoleId) > 0;
    }

    /**
     * 查询角色权限ID列表。
     *
     * @param tenantId 租户ID
     * @param roleId 角色ID
     * @return 权限ID列表
     */
    @Override
    public List<Long> findRolePermissionIds(String tenantId, Long roleId) {
        return identityMapper.findRolePermissionIds(tenantId, roleId);
    }

    /**
     * 重建角色权限绑定。
     *
     * @param tenantId 租户ID
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @param grantedBy 授权人
     */
    @Override
    public void replaceRolePermissions(String tenantId, Long roleId, List<Long> permissionIds, Long grantedBy) {
        identityMapper.deleteRolePermissions(tenantId, roleId);
        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }
        for (Long permissionId : permissionIds) {
            identityMapper.insertRolePermission(tenantId, roleId, permissionId, grantedBy);
        }
    }

    /**
     * 分页查询权限。
     *
     * @param query 查询条件
     * @return 权限列表
     */
    @Override
    public List<SysPermission> findPermissionPage(PermissionPageQuery query) {
        return identityMapper.findPermissionPage(query);
    }

    /**
     * 统计权限数量。
     *
     * @param query 查询条件
     * @return 总数
     */
    @Override
    public long countPermission(PermissionPageQuery query) {
        return identityMapper.countPermission(query);
    }

    /**
     * 校验权限ID集合是否全部存在。
     *
     * @param permissionIds 权限ID集合
     * @return 命中数量
     */
    @Override
    public long countPermissionByIds(List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return 0L;
        }
        return identityMapper.countPermissionByIds(permissionIds);
    }

    /**
     * 查询用户角色ID列表。
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 角色ID列表
     */
    @Override
    public List<Long> findUserRoleIds(String tenantId, Long userId) {
        List<Long> roleIds = identityMapper.findUserRoleIds(tenantId, userId);
        if (roleIds == null) {
            return Collections.emptyList();
        }
        return roleIds;
    }

    /**
     * 校验角色ID集合在租户内是否全部存在。
     *
     * @param tenantId 租户ID
     * @param roleIds 角色ID集合
     * @return 命中数量
     */
    @Override
    public long countRolesByIds(String tenantId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return 0L;
        }
        return identityMapper.countRolesByIds(tenantId, roleIds);
    }

    /**
     * 重建用户角色绑定。
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param roleIds 角色ID集合
     * @param grantedBy 授权人
     */
    @Override
    public void replaceUserRoles(String tenantId, Long userId, List<Long> roleIds, Long grantedBy) {
        identityMapper.deleteUserRoles(tenantId, userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (Long roleId : roleIds) {
            identityMapper.insertUserRole(tenantId, userId, roleId, grantedBy);
        }
    }
}
