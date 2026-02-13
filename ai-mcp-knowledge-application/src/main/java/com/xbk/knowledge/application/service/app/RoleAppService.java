package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.model.entity.SysRole;
import com.xbk.knowledge.domain.model.vo.identity.RolePageQuery;
import com.xbk.knowledge.types.common.PageResult;

import java.util.List;

/**
 * 角色管理应用服务接口。
 *
 * 职责：应用层用例接口，用于封装角色管理能力。
 *
 * @author xiexu
 */
public interface RoleAppService {

    /**
     * 分页查询角色。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<SysRole> queryRolePage(RolePageQuery query);

    /**
     * 创建角色。
     *
     * @param role 角色实体
     * @return 创建后的角色
     */
    SysRole createRole(SysRole role);

    /**
     * 更新角色。
     *
     * @param role 角色实体
     * @return 更新后的角色
     */
    SysRole updateRole(SysRole role);

    /**
     * 查询角色已分配权限ID列表。
     *
     * @param roleId 角色ID
     * @return 权限ID列表
     */
    List<Long> queryPermissionIds(Long roleId);

    /**
     * 绑定角色权限。
     *
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @param operatorId 操作人ID
     */
    void grantPermissions(Long roleId, List<Long> permissionIds, Long operatorId);
}
