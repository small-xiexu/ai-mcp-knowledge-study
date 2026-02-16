package com.xbk.knowledge.infrastructure.mapper.workflow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowEdge;
import com.xbk.knowledge.domain.model.vo.workflow.WorkflowGraphQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * WorkflowEdgeMapper。
 *
 * @author xiexu
 */
@Mapper
public interface WorkflowEdgeMapper extends BaseMapper<WorkflowEdge> {

    int insertEdge(WorkflowEdge edge);

    int deleteByVersion(@Param("orgId") Long orgId, @Param("workflowVersionId") Long workflowVersionId);

    List<WorkflowEdge> listEdges(WorkflowGraphQuery query);
}

