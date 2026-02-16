package com.xbk.knowledge.domain.repository.workflow;

import com.xbk.knowledge.domain.model.entity.workflow.WorkflowEdge;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowNode;
import com.xbk.knowledge.domain.model.vo.workflow.WorkflowGraphQuery;

import java.util.List;

/**
 * Workflow 图（节点+边）仓储接口。
 
  * @author xiexu
  */
public interface WorkflowGraphRepository {

    List<WorkflowNode> listNodes(WorkflowGraphQuery query);

    List<WorkflowEdge> listEdges(WorkflowGraphQuery query);

    /**
     * 用最新的 nodes/edges 覆盖保存（同一 version 内全量替换）。
     */
    void replaceGraph(Long orgId, Long workflowVersionId, List<WorkflowNode> nodes, List<WorkflowEdge> edges);
}

