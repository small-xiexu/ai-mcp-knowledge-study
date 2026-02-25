package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.identity.model.entity.SysPermission;
import com.xbk.knowledge.domain.identity.model.valobj.PermissionPageQuery;
import com.xbk.knowledge.types.common.PageResult;

/**
 * 权限管理应用服务接口。
 *
 * 职责：应用层用例接口，用于封装权限查询能力。
 *
 * @author sxie
 */
public interface PermissionAppService {

    /**
     * 分页查询权限。
     * 
     * @param query 分页查询条件。
     * @return SysPermission 分页结果。
     */
    PageResult<SysPermission> queryPermissionPage(PermissionPageQuery query);
}
