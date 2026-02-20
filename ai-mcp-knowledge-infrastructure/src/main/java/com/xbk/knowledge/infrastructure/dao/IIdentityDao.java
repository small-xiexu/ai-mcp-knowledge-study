package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.SysUserPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.identity.model.entity.SysPermission;
import com.xbk.knowledge.domain.identity.model.entity.SysRole;
import com.xbk.knowledge.domain.identity.model.entity.SysUser;
import com.xbk.knowledge.domain.identity.model.valobj.PermissionPageQuery;
import com.xbk.knowledge.domain.identity.model.valobj.RolePageQuery;
import com.xbk.knowledge.domain.identity.model.valobj.UserPageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 身份域 Mapper。
 *
 * 职责：MyBatis Mapper 接口，用于执行身份域 SQL。
 *
 * @author sxie
 */
@Mapper
public interface IIdentityDao extends BaseMapper<SysUserPO> {

    /**
     * 按用户名查询用户。
     *
     * @param username 用户名
     * @return 用户实体
     */
    SysUser findByUsername(@Param("username") String username);

    /**
     * 按用户ID查询用户。
     *
     * @param userId 用户ID
     * @return 用户实体
     */
    SysUser findById(@Param("userId") Long userId);

    /**
     * 查询角色编码列表。
     *
     * @param userId 用户ID
     * @return 角色编码列表
     */
    List<String> findRoleCodes(@Param("userId") Long userId);

    /**
     * 查询权限编码列表。
     *
     * @param userId 用户ID
     * @return 权限编码列表
     */
    List<String> findPermissionCodes(@Param("userId") Long userId);

    /**
     * 查询全部启用权限编码。
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
     * 统计用户总数。
     *
     * @param query 查询条件
     * @return 用户总数
     */
    long count(UserPageQuery query);

    /**
     * 插入用户。
     *
     * @param user 用户实体
     * @return 影响行数
     */
    int insertUser(SysUser user);

    /**
     * 更新用户基础信息。
     *
     * @param user 用户实体
     * @return 影响行数
     */
    int updateUser(SysUser user);

    /**
     * 判断用户名是否存在。
     *
     * @param username 用户名
     * @return 数量
     */
    long countByUsername(@Param("username") String username);

    /**
     * 更新用户最后登录信息。
     *
     * @param userId 用户ID
     * @param loginIp 登录IP
     * @param loginTime 登录时间
     * @return 影响行数
     */
    int updateLastLogin(@Param("userId") Long userId,
                        @Param("loginIp") String loginIp,
                        @Param("loginTime") LocalDateTime loginTime);

    /**
     * 更新用户密码哈希。
     *
     * @param userId 用户ID
     * @param passwordHash 密码哈希
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int updatePassword(@Param("userId") Long userId,
                       @Param("passwordHash") String passwordHash,
                       @Param("updateTime") LocalDateTime updateTime);

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
     * @return 影响行数
     */
    int insertRole(SysRole role);

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
     * @return 角色实体
     */
    SysRole findRoleById(@Param("roleId") Long roleId);

    /**
     * 统计角色编码数量。
     *
     * @param roleCode 角色编码
     * @param excludeRoleId 排除角色ID
     * @return 数量
     */
    long countByRoleCode(@Param("roleCode") String roleCode,
                         @Param("excludeRoleId") Long excludeRoleId);

    /**
     * 查询角色绑定权限ID列表。
     *
     * @param roleId 角色ID
     * @return 权限ID列表
     */
    List<Long> findRolePermissionIds(@Param("roleId") Long roleId);

    /**
     * 删除角色权限关系。
     *
     * @param roleId 角色ID
     * @return 影响行数
     */
    int deleteRolePermissions(@Param("roleId") Long roleId);

    /**
     * 插入角色权限关系。
     *
     * @param roleId 角色ID
     * @param permissionId 权限ID
     * @param grantedBy 授权人ID
     * @return 影响行数
     */
    int insertRolePermission(@Param("roleId") Long roleId,
                             @Param("permissionId") Long permissionId,
                             @Param("grantedBy") Long grantedBy);

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
     * 统计权限ID数量。
     *
     * @param permissionIds 权限ID集合
     * @return 命中数量
     */
    long countPermissionByIds(@Param("permissionIds") List<Long> permissionIds);

    /**
     * 查询用户角色ID列表。
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<Long> findUserRoleIds(@Param("userId") Long userId);

    /**
     * 统计角色ID数量。
     *
     * @param roleIds 角色ID集合
     * @return 命中数量
     */
    long countRolesByIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 删除用户角色关系。
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    int deleteUserRoles(@Param("userId") Long userId);

    /**
     * 插入用户角色关系。
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @param grantedBy 授权人
     * @return 影响行数
     */
    int insertUserRole(@Param("userId") Long userId,
                       @Param("roleId") Long roleId,
                       @Param("grantedBy") Long grantedBy);
}
