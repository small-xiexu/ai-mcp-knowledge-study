package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.SysOrg;
import com.xbk.knowledge.domain.model.vo.identity.OrgQuery;
import com.xbk.knowledge.domain.repository.OrgRepository;
import com.xbk.knowledge.infrastructure.mapper.OrgMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 组织仓储实现。
 *
 * 职责：基础设施层实现，用于落地组织数据访问。
 *
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class OrgRepositoryImpl implements OrgRepository {

    private final OrgMapper orgMapper;

    /**
     * 查询组织列表。
     *
     * @param query 查询条件
     * @return 组织列表
     */
    @Override
    public List<SysOrg> findList(OrgQuery query) {
        return orgMapper.findList(query);
    }

    /**
     * 按ID查询组织。
     *
     * @param orgId 组织ID
     * @return 组织
     */
    @Override
    public Optional<SysOrg> findById(Long orgId) {
        return Optional.ofNullable(orgMapper.findById(orgId));
    }

    /**
     * 新增组织。
     *
     * @param org 组织实体
     * @return 新增后的组织
     */
    @Override
    public SysOrg insert(SysOrg org) {
        orgMapper.insertOrg(org);
        return org;
    }

    /**
     * 更新组织。
     *
     * @param org 组织实体
     * @return 影响行数
     */
    @Override
    public int update(SysOrg org) {
        return orgMapper.updateOrg(org);
    }

    /**
     * 校验组织编码是否存在。
     *
     * @param tenantId 租户ID
     * @param orgCode 组织编码
     * @param excludeId 排除ID
     * @return 是否存在
     */
    @Override
    public boolean existsOrgCode(String tenantId, String orgCode, Long excludeId) {
        return orgMapper.countByTenantAndOrgCode(tenantId, orgCode, excludeId) > 0;
    }

    /**
     * 绑定用户主组织。
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param orgId 组织ID
     */
    @Override
    public void bindPrimaryOrg(String tenantId, Long userId, Long orgId) {
        orgMapper.deleteUserOrgs(tenantId, userId);
        orgMapper.insertUserOrg(tenantId, userId, orgId, 1);
    }
}
