package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.WorkflowVersionPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowVersionIdQuery;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowVersionListQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * IWorkflowVersion 数据访问接口定义。
 *
 * @author sxie
 */
@Mapper
public interface IWorkflowVersionDao extends BaseMapper<WorkflowVersionPO> {

    int insertVersion(WorkflowVersionPO version);

    int updateVersion(WorkflowVersionPO version);

    WorkflowVersionPO findById(WorkflowVersionIdQuery query);

    List<WorkflowVersionPO> listByWorkflowId(WorkflowVersionListQuery query);

    WorkflowVersionPO findPublishedVersion(@Param("workflowId") Long workflowId);
}

