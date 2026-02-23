package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.WorkflowEdgePO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowGraphQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * IWorkflowEdge 数据访问接口定义。
 *
 * @author sxie
 */
@Mapper
public interface IWorkflowEdgeDao extends BaseMapper<WorkflowEdgePO> {

    int insertEdge(WorkflowEdgePO edge);

    int deleteByVersion(@Param("workflowVersionId") Long workflowVersionId);

    List<WorkflowEdgePO> listEdges(WorkflowGraphQuery query);
}

