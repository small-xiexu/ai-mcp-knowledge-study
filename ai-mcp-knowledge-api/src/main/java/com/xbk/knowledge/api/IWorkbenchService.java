package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.workbench.WorkbenchSummaryResponse;
import com.xbk.knowledge.types.common.Result;

/**
 * 工作台服务接口
 * 定义工作台汇总视图的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IWorkbenchService {

    /**
     * 查询工作台汇总信息。
     *
     * @param ignored 占位参数
     * @return 查询结果
     */
    Result<WorkbenchSummaryResponse> summary(Object ignored);
}
