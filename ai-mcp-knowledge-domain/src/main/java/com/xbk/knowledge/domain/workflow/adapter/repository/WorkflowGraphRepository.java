package com.xbk.knowledge.domain.workflow.adapter.repository;

import com.xbk.knowledge.domain.workflow.model.entity.WorkflowEdge;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowNode;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowGraphQuery;

import java.util.List;

/**
 * Workflow 图（节点+边）仓储接口。
 *
 * @author sxie
 */
public interface WorkflowGraphRepository {

    /**
     * 查询流程图节点集合。
     */
    List<WorkflowNode> listNodes(WorkflowGraphQuery query);

    /**
     * 查询流程图边集合。
     */
    List<WorkflowEdge> listEdges(WorkflowGraphQuery query);

    /**
     * 用最新的 nodes/edges 覆盖保存（同一 version 内全量替换）。
     */
    void replaceGraph(Long workflowVersionId, List<WorkflowNode> nodes, List<WorkflowEdge> edges);
}

