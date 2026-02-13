package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.model.entity.SysOrg;
import com.xbk.knowledge.domain.model.vo.identity.OrgQuery;

import java.util.List;

/**
 * 组织管理应用服务接口。
 *
 * 职责：应用层用例接口，用于封装组织管理能力。
 *
 * @author xiexu
 */
public interface OrgAppService {

    /**
     * 查询组织列表。
     *
     * @param query 查询条件
     * @return 组织列表
     */
    List<SysOrg> queryList(OrgQuery query);

    /**
     * 创建组织。
     *
     * @param org 组织实体
     * @return 创建后的组织
     */
    SysOrg createOrg(SysOrg org);

    /**
     * 更新组织。
     *
     * @param org 组织实体
     * @return 更新后的组织
     */
    SysOrg updateOrg(SysOrg org);

    /**
     * 绑定用户主组织。
     *
     * @param userId 用户ID
     * @param orgId 组织ID
     */
    void bindUserPrimaryOrg(Long userId, Long orgId);
}
