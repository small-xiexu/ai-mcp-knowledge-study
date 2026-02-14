package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.OrgContextService;
import com.xbk.knowledge.domain.model.entity.SysUser;
import com.xbk.knowledge.domain.repository.IdentityRepository;
import com.xbk.knowledge.domain.repository.OrgRepository;
import com.xbk.knowledge.types.context.OrgContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 组织上下文解析实现。
 *
 * 规则：
 * - 普通用户：currentOrgId = operatorOrgId（忽略 targetOrgId）
 * - 超级管理员：currentOrgId = targetOrgId（若提供）；否则 currentOrgId = operatorOrgId 且 explicitTargetOrg=false
 */
@Service
@RequiredArgsConstructor
public class OrgContextServiceImpl implements OrgContextService {

    private static final long DEFAULT_ROOT_ORG_ID = 1L;

    private final IdentityRepository identityRepository;
    private final OrgRepository orgRepository;

    @Override
    public OrgContext resolve(Long userId, String targetOrgIdText) {
        Long operatorUserId = userId;
        boolean superAdmin = isSuperAdmin(userId);
        Long operatorOrgId = resolveOperatorOrgId(userId);
        Long targetOrgId = parseTargetOrgId(targetOrgIdText);
        boolean explicitTargetOrg = targetOrgId != null;

        Long currentOrgId;
        if (superAdmin) {
            currentOrgId = targetOrgId != null ? targetOrgId : operatorOrgId;
        } else {
            currentOrgId = operatorOrgId;
        }

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
        if (userId == null) {
            return DEFAULT_ROOT_ORG_ID;
        }
        return orgRepository.findPrimaryOrgId(userId).orElse(DEFAULT_ROOT_ORG_ID);
    }

    private Long parseTargetOrgId(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

