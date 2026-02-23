package com.xbk.knowledge.infrastructure.workflow.repository;

import com.xbk.knowledge.domain.workflow.model.entity.WorkflowEdge;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowNode;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowGraphQuery;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowGraphRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IWorkflowEdgeDao;
import com.xbk.knowledge.infrastructure.dao.IWorkflowNodeDao;
import com.xbk.knowledge.infrastructure.dao.po.WorkflowEdgePO;
import com.xbk.knowledge.infrastructure.dao.po.WorkflowNodePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

/**
 * 根据筛选条件查询工作流图列表。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class WorkflowGraphRepositoryImpl implements WorkflowGraphRepository {

    private final IWorkflowNodeDao nodeMapper;
    private final IWorkflowEdgeDao edgeMapper;

    /**
     * 根据筛选条件查询工作流图列表。
     *
     * @param query 查询条件
     * @return 返回 WorkflowNode 列表数据。
     */
    @Override
    public List<WorkflowNode> listNodes(WorkflowGraphQuery query) {
        if (query == null || query.getWorkflowVersionId() == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(nodeMapper.listNodes(query), WorkflowNode.class);
    }

    /**
     * 根据筛选条件查询工作流图列表。
     *
     * @param query 查询条件
     * @return 返回 WorkflowEdge 列表数据。
     */
    @Override
    public List<WorkflowEdge> listEdges(WorkflowGraphQuery query) {
        if (query == null || query.getWorkflowVersionId() == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(edgeMapper.listEdges(query), WorkflowEdge.class);
    }

    /**
     * 替换指定工作流版本的节点与边定义。
     *
     * @param workflowVersionId 工作流版本 ID。
     * @param nodes 节点列表。
     * @param edges 边列表。
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
                nodeMapper.insertNode(BeanMappingUtils.map(n, WorkflowNodePO.class));
            }
        }
        if (edges != null) {
            for (WorkflowEdge e : edges) {
                if (e == null) {
                    continue;
                }
                e.setWorkflowVersionId(workflowVersionId);
                edgeMapper.insertEdge(BeanMappingUtils.map(e, WorkflowEdgePO.class));
            }
        }
    }
}
