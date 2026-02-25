package com.xbk.knowledge.infrastructure.identity.repository;

import com.xbk.knowledge.domain.identity.model.entity.SysPermission;
import com.xbk.knowledge.domain.identity.model.entity.SysRole;
import com.xbk.knowledge.domain.identity.model.entity.SysUser;
import com.xbk.knowledge.domain.identity.model.valobj.PermissionPageQuery;
import com.xbk.knowledge.domain.identity.model.valobj.RolePageQuery;
import com.xbk.knowledge.domain.identity.model.valobj.UserPageQuery;
import com.xbk.knowledge.domain.identity.adapter.repository.IdentityRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IIdentityDao;
import com.xbk.knowledge.infrastructure.dao.po.SysPermissionPO;
import com.xbk.knowledge.infrastructure.dao.po.SysRolePO;
import com.xbk.knowledge.infrastructure.dao.po.SysUserPO;
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
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class IdentityRepositoryImpl implements IdentityRepository {

    /**
     * 身份域数据访问对象。
     */
    private final IIdentityDao identityMapper;

    /**
     * 按用户名查询用户。
     * 
     * @param username 用户名
     * @return 用户
     */
    @Override
    public Optional<SysUser> findByUsername(String username) {
        SysUser user = BeanMappingUtils.map(identityMapper.findByUsername(username), SysUser.class);
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
        SysUser user = BeanMappingUtils.map(identityMapper.findById(userId), SysUser.class);
        return Optional.ofNullable(user);
    }

    /**
     * 查询用户的角色编码集合。
     * 
     * @param userId 用户ID
     * @return 角色编码列表
     */
    @Override
    public List<String> findRoleCodes(Long userId) {
        return identityMapper.findRoleCodes(userId);
    }

    /**
     * 查询用户的权限编码集合。
     * 
     * @param userId 用户ID
     * @return 权限编码列表
     */
    @Override
    public List<String> findPermissionCodes(Long userId) {
        return identityMapper.findPermissionCodes(userId);
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
     * @param query 分页查询条件。
     * @return 用户列表
     */
    @Override
    public List<SysUser> findPage(UserPageQuery query) {
        return BeanMappingUtils.mapList(identityMapper.findPage(query), SysUser.class);
    }

    /**
     * 统计用户数量。
     * 
     * @param query 分页查询条件。
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
        identityMapper.insertUser(BeanMappingUtils.map(user, SysUserPO.class));
        return user;
    }

    /**
     * 更新用户基础信息。
     * 
     * @param user 用户实体
     * @return 影响行数
     */
    @Override
    public int updateUser(SysUser user) {
        return identityMapper.updateUser(BeanMappingUtils.map(user, SysUserPO.class));
    }

    /**
     * 判断用户名是否已存在。
     * 
     * @param username 用户名
     * @return 是否存在
     */
    @Override
    public boolean existsByUsername(String username) {
        return identityMapper.countByUsername(username) > 0;
    }

    /**
     * 更新最后登录信息。
     * 
     * @param userId 用户ID
     * @param loginIp 登录IP
     * @param loginTime 登录时间
     * @return 影响行数
     */
    @Override
    public int updateLastLogin(Long userId, String loginIp, LocalDateTime loginTime) {
        return identityMapper.updateLastLogin(userId, loginIp, loginTime);
    }

    /**
     * 更新用户密码哈希。
     * 
     * @param userId 用户ID
     * @param passwordHash 密码哈希
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Override
    public int updatePassword(Long userId, String passwordHash, LocalDateTime updateTime) {
        return identityMapper.updatePassword(userId, passwordHash, updateTime);
    }

    /**
     * 分页查询角色。
     * 
     * @param query 分页查询条件。
     * @return 角色列表
     */
    @Override
    public List<SysRole> findRolePage(RolePageQuery query) {
        return BeanMappingUtils.mapList(identityMapper.findRolePage(query), SysRole.class);
    }

    /**
     * 统计角色数量。
     * 
     * @param query 分页查询条件。
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
        identityMapper.insertRole(BeanMappingUtils.map(role, SysRolePO.class));
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
        return identityMapper.updateRole(BeanMappingUtils.map(role, SysRolePO.class));
    }

    /**
     * 按ID查询角色。
     * 
     * @param roleId 角色ID
     * @return 角色
     */
    @Override
    public Optional<SysRole> findRoleById(Long roleId) {
        return Optional.ofNullable(identityMapper.findRoleById(roleId))
                .map(item -> BeanMappingUtils.map(item, SysRole.class));
    }

    /**
     * 判断角色编码是否存在。
     * 
     * @param roleCode 角色编码
     * @param excludeRoleId 排除的角色ID
     * @return 是否存在
     */
    @Override
    public boolean existsRoleCode(String roleCode, Long excludeRoleId) {
        return identityMapper.countByRoleCode(roleCode, excludeRoleId) > 0;
    }

    /**
     * 查询角色绑定的权限 ID 列表。
     * 
     * @param roleId 角色ID
     * @return 权限ID列表
     */
    @Override
    public List<Long> findRolePermissionIds(Long roleId) {
        return identityMapper.findRolePermissionIds(roleId);
    }

    /**
     * 重建角色权限绑定。
     * 
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @param grantedBy 授权人
     */
    @Override
    public void replaceRolePermissions(Long roleId, List<Long> permissionIds, Long grantedBy) {
        identityMapper.deleteRolePermissions(roleId);
        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }
        for (Long permissionId : permissionIds) {
            identityMapper.insertRolePermission(roleId, permissionId, grantedBy);
        }
    }

    /**
     * 分页查询权限。
     * 
     * @param query 分页查询条件。
     * @return 权限列表
     */
    @Override
    public List<SysPermission> findPermissionPage(PermissionPageQuery query) {
        return BeanMappingUtils.mapList(identityMapper.findPermissionPage(query), SysPermission.class);
    }

    /**
     * 统计权限数量。
     * 
     * @param query 分页查询条件。
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
     * 查询用户绑定的角色 ID 列表。
     * 
     * @param userId 用户ID
     * @return 角色ID列表
     */
    @Override
    public List<Long> findUserRoleIds(Long userId) {
        List<Long> roleIds = identityMapper.findUserRoleIds(userId);
        if (roleIds == null) {
            return Collections.emptyList();
        }
        return roleIds;
    }

    /**
     * 校验角色ID集合是否全部存在。
     * 
     * @param roleIds 角色ID集合
     * @return 命中数量
     */
    @Override
    public long countRolesByIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return 0L;
        }
        return identityMapper.countRolesByIds(roleIds);
    }

    /**
     * 重建用户角色绑定。
     * 
     * @param userId 用户ID
     * @param roleIds 角色ID集合
     * @param grantedBy 授权人
     */
    @Override
    public void replaceUserRoles(Long userId, List<Long> roleIds, Long grantedBy) {
        identityMapper.deleteUserRoles(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (Long roleId : roleIds) {
            identityMapper.insertUserRole(userId, roleId, grantedBy);
        }
    }
}
