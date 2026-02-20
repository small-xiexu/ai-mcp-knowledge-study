package com.xbk.knowledge.infrastructure.workflow.repository;

import com.xbk.knowledge.domain.workflow.model.entity.WorkflowEdge;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowNode;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowGraphQuery;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowGraphRepository;
import com.xbk.knowledge.infrastructure.dao.IWorkflowEdgeDao;
import com.xbk.knowledge.infrastructure.dao.IWorkflowNodeDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

/**
 * WorkflowGraphRepositoryImpl。
 *
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class WorkflowGraphRepositoryImpl implements WorkflowGraphRepository {

    private final IWorkflowNodeDao nodeMapper;
    private final IWorkflowEdgeDao edgeMapper;

    /**
     * listNodes。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public List<WorkflowNode> listNodes(WorkflowGraphQuery query) {
        if (query == null || query.getWorkflowVersionId() == null) {
            return Collections.emptyList();
        }
        return nodeMapper.listNodes(query);
    }

    /**
     * listEdges。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public List<WorkflowEdge> listEdges(WorkflowGraphQuery query) {
        if (query == null || query.getWorkflowVersionId() == null) {
            return Collections.emptyList();
        }
        return edgeMapper.listEdges(query);
    }

    /**
     * replaceGraph。
     *
     * @param scopeId 参数
     * @param workflowVersionId 参数
     * @param nodes 参数
     * @param edges 参数
     */
    @Override
    public void replaceGraph(Long workflowVersionId, List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        if (workflowVersionId == null) {
            return;
        }
        nodeMapper.deleteByVersion(workflowVersionId);
        edgeMapper.deleteByVersion(workflowVersionId);

        if (nodes != null) {
            for (WorkflowNode n : nodes) {
                if (n == null) {
                    continue;
                }
                n.setWorkflowVersionId(workflowVersionId);
                nodeMapper.insertNode(n);
            }
        }
        if (edges != null) {
            for (WorkflowEdge e : edges) {
                if (e == null) {
                    continue;
                }
                e.setWorkflowVersionId(workflowVersionId);
                edgeMapper.insertEdge(e);
            }
        }
    }
}
