package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.WorkflowNodePO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowNode;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowGraphQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * IWorkflowNodeDao。
 *
 * @author sxie
 */
@Mapper
public interface IWorkflowNodeDao extends BaseMapper<WorkflowNodePO> {

    int insertNode(WorkflowNode node);

    int deleteByVersion(@Param("workflowVersionId") Long workflowVersionId);

    List<WorkflowNode> listNodes(WorkflowGraphQuery query);
}

