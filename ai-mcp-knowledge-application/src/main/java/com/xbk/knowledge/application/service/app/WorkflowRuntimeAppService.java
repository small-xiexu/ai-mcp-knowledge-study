package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.model.entity.workflow.WorkflowNodeRun;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowRun;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.contract.PlatformContractV1;

import java.util.List;

/**
 * Workflow 运行面应用服务。
 
  * @author xiexu
  */
public interface WorkflowRuntimeAppService {

    PlatformContractV1 run(Long orgId,
                           String workflowCode,
                           Long sessionId,
                           String content,
                           String variablesJson,
                           Long workflowVersionId);

    /**
     * 审批通过后的续跑入口（方式B）。
     */
    PlatformContractV1 resumeFromApproval(Long orgId, Long approvalRequestId);

    WorkflowRun getRun(Long orgId, String runId);

    PageResult<com.xbk.knowledge.domain.model.entity.workflow.WorkflowRun> listRuns(Long orgId, String status, int offset, int pageSize);

    List<WorkflowNodeRun> listNodeRuns(Long orgId, String runId);
}
