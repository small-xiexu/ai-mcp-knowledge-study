package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.WorkflowEdgePO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowEdge;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowGraphQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * IWorkflowEdgeDao。
 *
 * @author xiexu
 */
@Mapper
public interface IWorkflowEdgeDao extends BaseMapper<WorkflowEdgePO> {

    int insertEdge(WorkflowEdge edge);

    int deleteByVersion(@Param("workflowVersionId") Long workflowVersionId);

    List<WorkflowEdge> listEdges(WorkflowGraphQuery query);
}

