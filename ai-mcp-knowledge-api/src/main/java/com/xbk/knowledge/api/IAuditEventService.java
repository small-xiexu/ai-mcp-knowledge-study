package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.audit.AuditEventQueryRequest;
import com.xbk.knowledge.api.dto.audit.AuditEventResponse;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;

/**
 * 审计事件服务接口
 * 定义审计事件查询的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IAuditEventService {

    /**
     * 分页查询数据列表。
     *
     * @param request 请求参数
     * @return 列表结果
     */
    Result<PageResult<AuditEventResponse>> list(AuditEventQueryRequest request);
}
