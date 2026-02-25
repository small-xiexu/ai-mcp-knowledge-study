package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.common.model.valobj.IdQuery;

/**
 * Gateway 管理应用服务。
 *
 * 职责：承载网关与工具的级联清理用例，避免控制器直连多仓储。
 *
 * @author sxie
 */
public interface GatewayManageAppService {

    /**
     * 删除网关实例（应用层级联清理）。
     * 
     * @param query 网关主键
     */
    void deleteGatewayInstance(IdQuery query);

    /**
     * 删除网关工具（应用层级联清理）。
     * 
     * @param query 工具主键
     */
    void deleteTool(IdQuery query);
}
