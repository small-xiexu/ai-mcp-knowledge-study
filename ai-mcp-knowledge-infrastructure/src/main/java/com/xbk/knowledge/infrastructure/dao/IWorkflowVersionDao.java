package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.WorkflowVersionPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowVersion;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowVersionIdQuery;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowVersionListQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * IWorkflowVersionDao。
 *
 * @author sxie
 */
@Mapper
public interface IWorkflowVersionDao extends BaseMapper<WorkflowVersionPO> {

    int insertVersion(WorkflowVersion version);

    int updateVersion(WorkflowVersion version);

    WorkflowVersion findById(WorkflowVersionIdQuery query);

    List<WorkflowVersion> listByWorkflowId(WorkflowVersionListQuery query);

    WorkflowVersion findPublishedVersion(@Param("workflowId") Long workflowId);
}

