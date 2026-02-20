package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.application.model.workbench.WorkbenchSummary;

/**
 * 工作台聚合应用服务（方案B）。
 *
 * 职责：聚合多模块数据，为前端工作台提供“一次请求拿到全量状态”的汇总能力。
 *
 * @author xiexu
 */
public interface WorkbenchAppService {

    /**
     * 获取工作台汇总信息。
     *
     * @return 工作台汇总
     */
    WorkbenchSummary summary();
}
