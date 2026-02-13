package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.entity.SysOrg;
import com.xbk.knowledge.domain.model.vo.identity.OrgQuery;

import java.util.List;
import java.util.Optional;

/**
 * 组织仓储接口。
 *
 * 职责：组织数据访问抽象。
 *
 * @author xiexu
 */
public interface OrgRepository {

    /**
     * 查询组织列表。
     *
     * @param query 查询条件
     * @return 组织列表
     */
    List<SysOrg> findList(OrgQuery query);

    /**
     * 按ID查询组织。
     *
     * @param orgId 组织ID
     * @return 组织
     */
    Optional<SysOrg> findById(Long orgId);

    /**
     * 新增组织。
     *
     * @param org 组织实体
     * @return 新增后的组织
     */
    SysOrg insert(SysOrg org);

    /**
     * 更新组织。
     *
     * @param org 组织实体
     * @return 影响行数
     */
    int update(SysOrg org);

    /**
     * 校验组织编码是否存在。
     *
     * @param tenantId 租户ID
     * @param orgCode 组织编码
     * @param excludeId 排除ID
     * @return 是否存在
     */
    boolean existsOrgCode(String tenantId, String orgCode, Long excludeId);

    /**
     * 绑定用户主组织。
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param orgId 组织ID
     */
    void bindPrimaryOrg(String tenantId, Long userId, Long orgId);
}
