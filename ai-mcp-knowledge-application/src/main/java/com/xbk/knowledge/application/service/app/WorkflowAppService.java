package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.workflow.model.entity.Workflow;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowVersion;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowNode;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowEdge;
import com.xbk.knowledge.types.common.PageResult;

import java.util.List;

/**
 * Workflow 控制面应用服务。
 *
 * @author sxie
 */
public interface WorkflowAppService {

    PageResult<Workflow> list(String keyword, int offset, int pageSize);

    Workflow get(Long id);

    Workflow create(Workflow workflow);

    Workflow update(Workflow workflow);

    WorkflowVersion createVersion(Long workflowId, String changeSummary);

    List<WorkflowVersion> listVersions(Long workflowId);

    WorkflowVersion getVersion(Long workflowVersionId);

    WorkflowVersion publishVersion(Long workflowVersionId);

    WorkflowVersion saveGraph(Long workflowVersionId,
                              String graphJson,
                              String defaultConfigJson,
                              List<WorkflowNode> nodes,
                              List<WorkflowEdge> edges);
}
