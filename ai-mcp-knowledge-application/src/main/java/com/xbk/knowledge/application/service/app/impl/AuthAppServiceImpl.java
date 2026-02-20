package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.model.identity.AuthProfile;
import com.xbk.knowledge.application.service.app.AuthAppService;
import com.xbk.knowledge.domain.identity.model.entity.SysUser;
import com.xbk.knowledge.domain.identity.adapter.repository.IdentityRepository;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证应用服务实现。
 *
 * 职责：应用层用例实现，用于编排认证与权限查询能力。
 *
 * @author xiexu
 */
@Service
@RequiredArgsConstructor
public class AuthAppServiceImpl implements AuthAppService {

    private final IdentityRepository identityRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    /**
     * 校验登录凭证并返回用户。
     *
     * @param username    用户名
     * @param rawPassword 明文密码
     * @return 用户实体
     */
    @Override
    public SysUser verifyLogin(String username, String rawPassword) {
        SysUser user = identityRepository
                .findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));
        validateUserStatus(user);
        boolean passwordMatched = bCryptPasswordEncoder.matches(rawPassword, user.getPasswordHash());
        if (!passwordMatched) {
            throw new BusinessException("用户名或密码错误");
        }
        return user;
    }

    /**
     * 记录登录成功信息。
     *
     * @param userId  用户ID
     * @param loginIp 登录IP
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordLoginSuccess(Long userId, String loginIp) {
        identityRepository.updateLastLogin(userId, loginIp, LocalDateTime.now());
    }

    /**
     * 加载登录用户画像。
     *
     * @param userId 用户ID
     * @return 用户画像
     */
    @Override
    public AuthProfile loadProfile(Long userId) {
        SysUser user = identityRepository
                .findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在，id: " + userId));
        List<String> roleCodes = queryRoleCodes(userId);
        List<String> permissionCodes = queryPermissionCodes(userId);
        return AuthProfile.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .superAdmin(isSuperAdmin(user))
                .roleCodes(roleCodes)
                .permissionCodes(permissionCodes)
                .build();
    }

    /**
     * 查询用户角色编码列表。
     *
     * @param userId 用户ID
     * @return 角色编码列表
     */
    @Override
    public List<String> queryRoleCodes(Long userId) {
        SysUser user = identityRepository
                .findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在，id: " + userId));
        return identityRepository.findRoleCodes(userId);
    }

    /**
     * 查询用户权限编码列表。
     *
     * @param userId 用户ID
     * @return 权限编码列表
     */
    @Override
    public List<String> queryPermissionCodes(Long userId) {
        SysUser user = identityRepository
                .findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在，id: " + userId));
        if (isSuperAdmin(user)) {
            return identityRepository.findAllPermissionCodes();
        }
        return identityRepository.findPermissionCodes(userId);
    }

    /**
     * 校验用户状态是否可登录。
     *
     * @param user 用户实体
     */
    private void validateUserStatus(SysUser user) {
        Integer status = user.getStatus();
        if (status == null || status != 1) {
            throw new BusinessException("账号已禁用或锁定");
        }
    }

    /**
     * 判断是否平台超管。
     *
     * @param user 用户实体
     * @return 是否超管
     */
    private boolean isSuperAdmin(SysUser user) {
        Integer isSuperAdmin = user.getIsSuperAdmin();
        return isSuperAdmin != null && isSuperAdmin == 1;
    }
}
