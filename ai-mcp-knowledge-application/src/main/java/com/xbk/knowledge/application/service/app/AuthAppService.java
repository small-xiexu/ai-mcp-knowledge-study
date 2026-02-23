package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.application.model.identity.AuthProfile;
import com.xbk.knowledge.domain.identity.model.entity.SysUser;

import java.util.List;

/**
 * 认证应用服务接口。
 *
 * 职责：应用层用例接口，用于封装认证与授权查询能力。
 *
 * @author sxie
 */
public interface AuthAppService {

    /**
     * 校验登录凭证并返回用户。
     *
     * @param username    用户名
     * @param rawPassword 明文密码
     * @return 用户实体
     */
    SysUser verifyLogin(String username, String rawPassword);

    /**
     * 记录登录成功信息。
     *
     * @param userId    用户ID
     * @param loginIp   登录IP
     */
    void recordLoginSuccess(Long userId, String loginIp);

    /**
     * 加载登录用户画像。
     *
     * @param userId 用户ID
     * @return 用户画像
     */
    AuthProfile loadProfile(Long userId);

    /**
     * 查询用户角色编码集合。
     *
     * @param userId 用户ID
     * @return 角色编码列表
     */
    List<String> queryRoleCodes(Long userId);

    /**
     * 查询用户权限编码集合。
     *
     * @param userId 用户ID
     * @return 权限编码列表
     */
    List<String> queryPermissionCodes(Long userId);
}
