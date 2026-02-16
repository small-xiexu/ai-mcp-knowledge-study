package com.xbk.knowledge.infrastructure.repository.workflow;

import com.xbk.knowledge.domain.model.entity.workflow.WorkflowEdge;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowNode;
import com.xbk.knowledge.domain.model.vo.workflow.WorkflowGraphQuery;
import com.xbk.knowledge.domain.repository.workflow.WorkflowGraphRepository;
import com.xbk.knowledge.infrastructure.mapper.workflow.WorkflowEdgeMapper;
import com.xbk.knowledge.infrastructure.mapper.workflow.WorkflowNodeMapper;
import com.xbk.knowledge.types.context.OrgContextHolder;
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

    private final WorkflowNodeMapper nodeMapper;
    private final WorkflowEdgeMapper edgeMapper;

    private Long currentOrgIdOrRoot() {
        Long orgId = OrgContextHolder.currentOrgIdOrNull();
        return orgId == null ? 1L : orgId;
    }

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
        if (query.getOrgId() == null) {
            query.setOrgId(currentOrgIdOrRoot());
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
        if (query.getOrgId() == null) {
            query.setOrgId(currentOrgIdOrRoot());
        }
        return edgeMapper.listEdges(query);
    }

    /**
     * replaceGraph。
     *
     * @param orgId 参数
     * @param workflowVersionId 参数
     * @param nodes 参数
     * @param edges 参数
     */
    @Override
    public void replaceGraph(Long orgId, Long workflowVersionId, List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        if (orgId == null) {
            orgId = currentOrgIdOrRoot();
        }
        if (workflowVersionId == null) {
            return;
        }
        nodeMapper.deleteByVersion(orgId, workflowVersionId);
        edgeMapper.deleteByVersion(orgId, workflowVersionId);

        if (nodes != null) {
            for (WorkflowNode n : nodes) {
                if (n == null) {
                    continue;
                }
                n.setOrgId(orgId);
                n.setWorkflowVersionId(workflowVersionId);
                nodeMapper.insertNode(n);
            }
        }
        if (edges != null) {
            for (WorkflowEdge e : edges) {
                if (e == null) {
                    continue;
                }
                e.setOrgId(orgId);
                e.setWorkflowVersionId(workflowVersionId);
                edgeMapper.insertEdge(e);
            }
        }
    }
}

