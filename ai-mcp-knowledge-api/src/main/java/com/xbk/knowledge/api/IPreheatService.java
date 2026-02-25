package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.preheat.PreheatAgentVersionRequest;
import com.xbk.knowledge.api.dto.preheat.PreheatResponse;
import com.xbk.knowledge.api.dto.preheat.PreheatWorkflowVersionRequest;
import com.xbk.knowledge.types.common.Result;

/**
 * 预热服务接口
 * 定义 Agent 与 Workflow 预热的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IPreheatService {

    /**
     * 预热 Agent 版本。
     * 
     * @param request Agent 版本预热参数。
     * @return 预热结果
     */
    Result<PreheatResponse> preheatAgentVersion(PreheatAgentVersionRequest request);

    /**
     * 预热 Workflow 版本。
     * 
     * @param request Workflow 版本预热参数。
     * @return 预热结果
     */
    Result<PreheatResponse> preheatWorkflowVersion(PreheatWorkflowVersionRequest request);
}
