package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.UserIdentityAppService;
import com.xbk.knowledge.domain.model.entity.SysUser;
import com.xbk.knowledge.domain.model.vo.identity.UserPageQuery;
import com.xbk.knowledge.domain.repository.identity.IdentityRepository;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
        Integer offset = query.getOffset() == null ? 0 : query.getOffset();
        Integer pageSize = query.getPageSize() == null ? 10 : query.getPageSize();
        UserPageQuery normalizedQuery = new UserPageQuery(
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
        String username = user.getUsername();
        if (identityRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在：" + username);
        }
        LocalDateTime now = LocalDateTime.now();
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
     * 更新用户基础信息。
     *
     * @param user 用户实体
     * @return 更新后的用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser updateUser(SysUser user) {
        SysUser existed = identityRepository
                .findById(user.getId())
                .orElseThrow(() -> new NotFoundException("用户不存在，id: " + user.getId()));
        LocalDateTime now = LocalDateTime.now();
        existed.setDisplayName(user.getDisplayName());
        existed.setEmail(user.getEmail());
        existed.setMobile(user.getMobile());
        existed.setStatus(user.getStatus());
        if (user.getIsSuperAdmin() != null) {
            existed.setIsSuperAdmin(user.getIsSuperAdmin());
        }
        existed.setUpdatedAt(now);
        int affected = identityRepository.updateUser(existed);
        if (affected <= 0) {
            throw new BusinessException("用户更新失败，id: " + user.getId());
        }
        return identityRepository.findById(user.getId()).orElse(existed);
    }

    /**
     * 重置用户密码。
     *
     * @param userId 用户ID
     * @param rawPassword 新密码明文
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String rawPassword) {
        identityRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在，id: " + userId));
        int affected = identityRepository.updatePassword(
                userId,
                bCryptPasswordEncoder.encode(rawPassword),
                LocalDateTime.now()
        );
        if (affected <= 0) {
            throw new BusinessException("密码重置失败，id: " + userId);
        }
    }

    /**
     * 查询用户已分配角色ID列表。
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    @Override
    public List<Long> queryRoleIds(Long userId) {
        identityRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在，id: " + userId));
        return identityRepository.findUserRoleIds(userId);
    }

    /**
     * 重新绑定用户角色。
     *
     * @param userId 目标用户ID
     * @param roleIds 角色ID集合
     * @param operatorId 操作人ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantRoles(Long userId, List<Long> roleIds, Long operatorId) {
        identityRepository
                .findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在，id: " + userId));
        if (roleIds != null && !roleIds.isEmpty()) {
            long roleCount = identityRepository.countRolesByIds(roleIds);
            if (roleCount != roleIds.size()) {
                throw new BusinessException("存在无效角色ID");
            }
        }
        identityRepository.replaceUserRoles(userId, roleIds, operatorId);
    }
}
