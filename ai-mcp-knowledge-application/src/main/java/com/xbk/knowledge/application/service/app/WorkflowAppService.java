package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.model.entity.workflow.Workflow;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowVersion;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowNode;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowEdge;
import com.xbk.knowledge.types.common.PageResult;

import java.util.List;

/**
 * Workflow 控制面应用服务。
 
  * @author xiexu
  */
public interface WorkflowAppService {

    PageResult<Workflow> list(Long orgId, String keyword, int offset, int pageSize);

    Workflow get(Long orgId, Long id);

    Workflow create(Long orgId, Workflow workflow);

    Workflow update(Long orgId, Workflow workflow);

    WorkflowVersion createVersion(Long orgId, Long workflowId, String changeSummary);

    List<WorkflowVersion> listVersions(Long orgId, Long workflowId);

    WorkflowVersion getVersion(Long orgId, Long workflowVersionId);

    WorkflowVersion publishVersion(Long orgId, Long workflowVersionId);

    WorkflowVersion saveGraph(Long orgId,
                              Long workflowVersionId,
                              String graphJson,
                              String defaultConfigJson,
                              List<WorkflowNode> nodes,
                              List<WorkflowEdge> edges);
}
