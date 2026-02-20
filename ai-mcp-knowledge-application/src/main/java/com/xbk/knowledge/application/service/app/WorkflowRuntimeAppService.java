package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.workflow.model.entity.WorkflowNodeRun;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowRun;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.contract.PlatformContractV1;

import java.util.List;

/**
 * Workflow 运行面应用服务。
 *
 * @author sxie
 */
public interface WorkflowRuntimeAppService {

    PlatformContractV1 run(String workflowCode,
                           Long sessionId,
                           String content,
                           String variablesJson,
                           Long workflowVersionId);

    /**
     * 审批通过后的续跑入口（方式B）。
     */
    PlatformContractV1 resumeFromApproval(Long approvalRequestId);

    WorkflowRun getRun(String runId);

    PageResult<WorkflowRun> listRuns(String status, int offset, int pageSize);

    List<WorkflowNodeRun> listNodeRuns(String runId);
}
