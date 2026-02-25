package com.xbk.knowledge.trigger.auth;

import cn.dev33.satoken.stp.StpInterface;
import com.xbk.knowledge.application.service.app.AuthAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 角色与权限查询提供者。
 *
 * 职责：触发层鉴权适配，用于将平台权限模型接入 Sa-Token。
 *
 * @author sxie
 */
@Component
@RequiredArgsConstructor
public class SaTokenPermissionProvider implements StpInterface {

    /**
     * 鉴权应用服务。
     */
    private final AuthAppService authAppService;

    /**
     * 查询权限列表。
     * 
     * @param loginId 登录ID
     * @param loginType 登录类型
     * @return 权限编码列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = parseLoginId(loginId);
        if (userId == null) {
            return Collections.emptyList();
        }
        return authAppService.queryPermissionCodes(userId);
    }

    /**
     * 查询角色列表。
     * 
     * @param loginId 登录ID
     * @param loginType 登录类型
     * @return 角色编码列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = parseLoginId(loginId);
        if (userId == null) {
            return Collections.emptyList();
        }
        return authAppService.queryRoleCodes(userId);
    }

    /**
     * 解析登录ID。
     * 
     * @param loginId 登录ID对象
     * @return 用户ID
     */
    private Long parseLoginId(Object loginId) {
        if (loginId == null) {
            return null;
        }
        String loginIdText = String.valueOf(loginId);
        try {
            return Long.parseLong(loginIdText);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
