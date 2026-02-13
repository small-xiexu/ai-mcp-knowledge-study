package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.OrgAppService;
import com.xbk.knowledge.domain.model.entity.SysOrg;
import com.xbk.knowledge.domain.model.entity.SysUser;
import com.xbk.knowledge.domain.model.vo.identity.OrgQuery;
import com.xbk.knowledge.domain.repository.IdentityRepository;
import com.xbk.knowledge.domain.repository.OrgRepository;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 组织管理应用服务实现。
 *
 * 职责：应用层用例实现，用于编排组织管理流程。
 *
 * @author xiexu
 */
@Service
@RequiredArgsConstructor
public class OrgAppServiceImpl implements OrgAppService {

    private static final String DEFAULT_ORG_SCOPE = "/ROOT";

    private final OrgRepository orgRepository;
    private final IdentityRepository identityRepository;

    /**
     * 查询组织列表。
     *
     * @param query 查询条件
     * @return 组织列表
     */
    @Override
    public List<SysOrg> queryList(OrgQuery query) {
        return orgRepository.findList(query);
    }

    /**
     * 创建组织。
     *
     * @param org 组织实体
     * @return 创建后的组织
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysOrg createOrg(SysOrg org) {
        if (!StringUtils.hasText(org.getOrgCode())) {
            throw new BusinessException("组织编码不能为空");
        }
        if (orgRepository.existsOrgCode(org.getOrgCode(), null)) {
            throw new BusinessException("组织编码已存在：" + org.getOrgCode());
        }
        if (!StringUtils.hasText(org.getOrgPath())) {
            org.setOrgPath(DEFAULT_ORG_SCOPE + "/" + org.getOrgCode());
        }
        if (org.getStatus() == null) {
            org.setStatus(1);
        }
        LocalDateTime now = LocalDateTime.now();
        org.setCreatedAt(now);
        org.setUpdatedAt(now);
        return orgRepository.insert(org);
    }

    /**
     * 更新组织。
     *
     * @param org 组织实体
     * @return 更新后的组织
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysOrg updateOrg(SysOrg org) {
        SysOrg existing = orgRepository
                .findById(org.getId())
                .orElseThrow(() -> new NotFoundException("组织不存在，id: " + org.getId()));
        existing.setOrgName(org.getOrgName());
        existing.setParentId(org.getParentId());
        existing.setOrgPath(org.getOrgPath());
        existing.setStatus(org.getStatus());
        existing.setRemark(org.getRemark());
        existing.setUpdatedAt(LocalDateTime.now());
        orgRepository.update(existing);
        return existing;
    }

    /**
     * 绑定用户主组织。
     *
     * @param userId 用户ID
     * @param orgId 组织ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindUserPrimaryOrg(Long userId, Long orgId) {
        SysUser user = identityRepository
                .findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在，id: " + userId));
        SysOrg org = orgRepository
                .findById(orgId)
                .orElseThrow(() -> new NotFoundException("组织不存在，id: " + orgId));
        orgRepository.bindPrimaryOrg(user.getId(), org.getId());
    }
}
