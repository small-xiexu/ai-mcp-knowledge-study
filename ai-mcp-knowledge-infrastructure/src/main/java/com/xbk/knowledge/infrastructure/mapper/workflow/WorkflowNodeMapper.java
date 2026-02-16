package com.xbk.knowledge.infrastructure.mapper.workflow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowNode;
import com.xbk.knowledge.domain.model.vo.workflow.WorkflowGraphQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * WorkflowNodeMapper。
 *
 * @author xiexu
 */
@Mapper
public interface WorkflowNodeMapper extends BaseMapper<WorkflowNode> {

    int insertNode(WorkflowNode node);

    int deleteByVersion(@Param("orgId") Long orgId, @Param("workflowVersionId") Long workflowVersionId);

    List<WorkflowNode> listNodes(WorkflowGraphQuery query);
}

