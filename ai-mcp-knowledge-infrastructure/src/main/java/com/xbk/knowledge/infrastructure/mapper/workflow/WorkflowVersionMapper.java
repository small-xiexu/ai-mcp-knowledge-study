package com.xbk.knowledge.infrastructure.mapper.workflow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowVersion;
import com.xbk.knowledge.domain.model.vo.workflow.WorkflowVersionIdQuery;
import com.xbk.knowledge.domain.model.vo.workflow.WorkflowVersionListQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * WorkflowVersionMapper。
 *
 * @author xiexu
 */
@Mapper
public interface WorkflowVersionMapper extends BaseMapper<WorkflowVersion> {

    int insertVersion(WorkflowVersion version);

    int updateVersion(WorkflowVersion version);

    WorkflowVersion findById(WorkflowVersionIdQuery query);

    List<WorkflowVersion> listByWorkflowId(WorkflowVersionListQuery query);

    WorkflowVersion findPublishedVersion(@Param("orgId") Long orgId, @Param("workflowId") Long workflowId);
}

