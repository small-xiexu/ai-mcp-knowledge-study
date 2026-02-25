package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.RoleAppService;
import com.xbk.knowledge.domain.identity.model.entity.SysRole;
import com.xbk.knowledge.domain.identity.model.valobj.RolePageQuery;
import com.xbk.knowledge.domain.identity.adapter.repository.IdentityRepository;
import com.xbk.knowledge.types.common.PageParamUtils;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色管理应用服务实现。
 *
 * 职责：应用层用例实现，用于编排角色管理流程。
 *
 * @author sxie
 */
@Service
@RequiredArgsConstructor
public class RoleAppServiceImpl implements RoleAppService {

    /**
     * 身份仓储。
     */
    private final IdentityRepository identityRepository;

    /**
     * 分页查询角色。
     * 
     * @param query 分页查询条件。
     * @return SysRole 分页结果。
     */
    @Override
    public PageResult<SysRole> queryRolePage(RolePageQuery query) {
        Integer offset = PageParamUtils.normalizeOffset(query.getOffset());
        Integer pageSize = PageParamUtils.normalizePageSize(query.getPageSize(), 10);
        RolePageQuery normalizedQuery = new RolePageQuery(
                query.getRoleCode(),
                query.getStatus(),
                offset,
                pageSize
        );
        List<SysRole> roles = identityRepository.findRolePage(normalizedQuery);
        long total = identityRepository.countRole(normalizedQuery);
        int pageNum = PageParamUtils.offsetToPageNum(offset, pageSize);
        return PageResult.of(roles, total, pageNum, pageSize);
    }

    /**
     * 创建角色。
     * 
     * @param role 角色实体
     * @return 创建后的角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRole createRole(SysRole role) {
        if (!StringUtils.hasText(role.getRoleCode())) {
            throw new BusinessException("角色编码不能为空");
        }
        if (identityRepository.existsRoleCode(role.getRoleCode(), null)) {
            throw new BusinessException("角色编码已存在" + role.getRoleCode());
        }
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        LocalDateTime now = LocalDateTime.now();
        role.setCreatedAt(now);
        role.setUpdatedAt(now);
        return identityRepository.insertRole(role);
    }

    /**
     * 更新角色。
     * 
     * @param role 角色实体
     * @return 更新后的角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRole updateRole(SysRole role) {
        Long roleId = role.getId();
        SysRole existing = identityRepository
                .findRoleById(roleId)
                .orElseThrow(() -> new NotFoundException("角色不存在，id: " + roleId));
        if (identityRepository.existsRoleCode(existing.getRoleCode(), roleId)) {
            throw new BusinessException("角色编码已存在" + existing.getRoleCode());
        }
        existing.setRoleName(role.getRoleName());
        existing.setStatus(role.getStatus());
        existing.setRemark(role.getRemark());
        existing.setUpdatedAt(LocalDateTime.now());
        identityRepository.updateRole(existing);
        return existing;
    }

    /**
     * 查询角色已绑定的权限 ID 列表。
     * 
     * @param roleId 角色ID
     * @return 权限ID列表
     */
    @Override
    public List<Long> queryPermissionIds(Long roleId) {
        identityRepository
                .findRoleById(roleId)
                .orElseThrow(() -> new NotFoundException("角色不存在，id: " + roleId));
        return identityRepository.findRolePermissionIds(roleId);
    }

    /**
     * 绑定角色权限。
     * 
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @param operatorId 操作人ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantPermissions(Long roleId, List<Long> permissionIds, Long operatorId) {
        identityRepository
                .findRoleById(roleId)
                .orElseThrow(() -> new NotFoundException("角色不存在，id: " + roleId));
        if (permissionIds != null && !permissionIds.isEmpty()) {
            long permissionCount = identityRepository.countPermissionByIds(permissionIds);
            if (permissionCount != permissionIds.size()) {
                throw new BusinessException("存在无效权限ID");
            }
        }
        identityRepository.replaceRolePermissions(roleId, permissionIds, operatorId);
    }

}
