package com.xbk.knowledge.trigger.security;

import cn.dev33.satoken.exception.NotPermissionException;
import com.xbk.knowledge.application.service.app.AuthAppService;
import com.xbk.knowledge.application.service.app.IdentityContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * XXL 权限守卫
 * 预留最小权限校验入口，便于后续接入认证与授权
 *
 * @author sxie
 */
@Component
@RequiredArgsConstructor
public class XxlPermissionGuard {

    /**
     * 当前用户身份上下文服务。
     */
    private final IdentityContextService identityContextService;

    /**
     * 鉴权应用服务。
     */
    private final AuthAppService authAppService;

    /**
     * 任务查看权限校验
     *
     * 接口层先校验权限，避免非法请求进入下游调用链。
     */
    public void assertCanView() {
        assertPermission("workflow:read");
    }

    /**
     * 任务编辑权限校验
     *
     * 编辑类操作风险更高，需要明确权限隔离。
     */
    public void assertCanEdit() {
        assertPermission("workflow:write");
    }

    private void assertPermission(String permissionCode) {
        Long currentUserId = identityContextService.getCurrentUserId();
        List<String> permissionCodes = authAppService.queryPermissionCodes(currentUserId);
        if (permissionCodes == null || !permissionCodes.contains(permissionCode)) {
            throw new NotPermissionException(permissionCode, null);
        }
    }
}
