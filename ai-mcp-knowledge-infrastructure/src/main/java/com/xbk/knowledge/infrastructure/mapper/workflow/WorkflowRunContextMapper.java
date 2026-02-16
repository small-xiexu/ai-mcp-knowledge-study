package com.xbk.knowledge.infrastructure.mapper.workflow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowRunContext;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * WorkflowRunContextMapper。
 *
 * @author xiexu
 */
@Mapper
public interface WorkflowRunContextMapper extends BaseMapper<WorkflowRunContext> {

    int upsert(WorkflowRunContext ctx);

    WorkflowRunContext findByRunId(@Param("orgId") Long orgId, @Param("runId") String runId);

    int updateStatus(@Param("orgId") Long orgId, @Param("runId") String runId, @Param("status") String status);

    int deleteByRunIds(@Param("orgId") Long orgId, @Param("runIds") List<String> runIds);
}
