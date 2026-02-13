package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.model.entity.SysPermission;
import com.xbk.knowledge.domain.model.vo.identity.PermissionPageQuery;
import com.xbk.knowledge.types.common.PageResult;

/**
 * 权限管理应用服务接口。
 *
 * 职责：应用层用例接口，用于封装权限查询能力。
 *
 * @author xiexu
 */
public interface PermissionAppService {

    /**
     * 分页查询权限。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<SysPermission> queryPermissionPage(PermissionPageQuery query);
}
