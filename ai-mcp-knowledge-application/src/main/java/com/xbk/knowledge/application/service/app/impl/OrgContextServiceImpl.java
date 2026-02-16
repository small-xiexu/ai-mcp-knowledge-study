package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.OrgContextService;
import com.xbk.knowledge.domain.model.entity.SysUser;
import com.xbk.knowledge.domain.repository.identity.IdentityRepository;
import com.xbk.knowledge.types.context.OrgContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 组织上下文解析实现。
 *
 * 规则：单组织模式下，operatorOrgId 与 currentOrgId 固定为 ROOT(1)。
 
  * @author xiexu
  */
@Service
@RequiredArgsConstructor
public class OrgContextServiceImpl implements OrgContextService {

    private static final long DEFAULT_ROOT_ORG_ID = 1L;

    private final IdentityRepository identityRepository;

    /**
     * resolve。
     *
     * @param userId 参数
     * @return 返回结果
     */
    @Override
    public OrgContext resolve(Long userId) {
        Long operatorUserId = userId;
        boolean superAdmin = isSuperAdmin(userId);
        Long operatorOrgId = resolveOperatorOrgId(userId);
        Long currentOrgId = operatorOrgId;
        boolean explicitTargetOrg = true;

        return new OrgContext(
                operatorUserId,
                operatorOrgId,
                currentOrgId,
                superAdmin,
                explicitTargetOrg
        );
    }

    private boolean isSuperAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        SysUser user = identityRepository.findById(userId).orElse(null);
        if (user == null || user.getIsSuperAdmin() == null) {
            return false;
        }
        return user.getIsSuperAdmin() == 1;
    }

    private Long resolveOperatorOrgId(Long userId) {
        return DEFAULT_ROOT_ORG_ID;
    }
}
