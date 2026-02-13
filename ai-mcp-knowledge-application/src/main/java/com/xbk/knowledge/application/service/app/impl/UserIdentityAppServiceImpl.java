package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.UserIdentityAppService;
import com.xbk.knowledge.domain.model.entity.SysUser;
import com.xbk.knowledge.domain.model.vo.identity.UserPageQuery;
import com.xbk.knowledge.domain.repository.IdentityRepository;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 用户管理应用服务实现。
 *
 * 职责：应用层用例实现，用于编排用户管理流程。
 *
 * @author xiexu
 */
@Service
@RequiredArgsConstructor
public class UserIdentityAppServiceImpl implements UserIdentityAppService {

    private static final String DEFAULT_TENANT_ID = "default";

    private final IdentityRepository identityRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    /**
     * 分页查询用户。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<SysUser> queryUserPage(UserPageQuery query) {
        String tenantId = resolveTenantId(query.getTenantId());
        Integer offset = query.getOffset() == null ? 0 : query.getOffset();
        Integer pageSize = query.getPageSize() == null ? 10 : query.getPageSize();
        UserPageQuery normalizedQuery = new UserPageQuery(
                tenantId,
                query.getUsername(),
                query.getStatus(),
                offset,
                pageSize
        );
        List<SysUser> users = identityRepository.findPage(normalizedQuery);
        long total = identityRepository.count(normalizedQuery);
        int pageNum = (offset / pageSize) + 1;
        return PageResult.of(users, total, pageNum, pageSize);
    }

    /**
     * 创建用户。
     *
     * @param user        用户实体
     * @param rawPassword 明文密码
     * @return 创建后的用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser createUser(SysUser user, String rawPassword) {
        String tenantId = resolveTenantId(user.getTenantId());
        String username = user.getUsername();
        if (identityRepository.existsByTenantAndUsername(tenantId, username)) {
            throw new BusinessException("用户名已存在：" + username);
        }
        LocalDateTime now = LocalDateTime.now();
        user.setTenantId(tenantId);
        user.setPasswordHash(bCryptPasswordEncoder.encode(rawPassword));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        if (user.getIsSuperAdmin() == null) {
            user.setIsSuperAdmin(0);
        }
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return identityRepository.insert(user);
    }

    /**
     * 查询用户已分配角色ID列表。
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 角色ID列表
     */
    @Override
    public List<Long> queryRoleIds(String tenantId, Long userId) {
        SysUser user = identityRepository
                .findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在，id: " + userId));
        if (!Objects.equals(user.getTenantId(), tenantId)) {
            throw new BusinessException("不允许跨租户查询角色");
        }
        return identityRepository.findUserRoleIds(tenantId, userId);
    }

    /**
     * 重新绑定用户角色。
     *
     * @param tenantId 操作租户ID
     * @param userId 目标用户ID
     * @param roleIds 角色ID集合
     * @param operatorId 操作人ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantRoles(String tenantId, Long userId, List<Long> roleIds, Long operatorId) {
        SysUser user = identityRepository
                .findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在，id: " + userId));
        if (!Objects.equals(user.getTenantId(), tenantId)) {
            throw new BusinessException("不允许跨租户分配角色");
        }
        if (roleIds != null && !roleIds.isEmpty()) {
            long roleCount = identityRepository.countRolesByIds(tenantId, roleIds);
            if (roleCount != roleIds.size()) {
                throw new BusinessException("存在无效角色ID");
            }
        }
        identityRepository.replaceUserRoles(tenantId, userId, roleIds, operatorId);
    }

    /**
     * 解析租户ID。
     *
     * @param tenantId 租户ID
     * @return 解析后的租户ID
     */
    private String resolveTenantId(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            return DEFAULT_TENANT_ID;
        }
        return tenantId.trim();
    }
}
