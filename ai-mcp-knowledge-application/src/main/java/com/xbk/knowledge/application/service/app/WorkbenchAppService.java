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
     * 获取当前 org 视角下的工作台汇总信息。
     *
     * @param orgId 当前请求的目标 orgId（资源归属）
     * @return 工作台汇总
     */
    WorkbenchSummary summary(Long orgId);
}

