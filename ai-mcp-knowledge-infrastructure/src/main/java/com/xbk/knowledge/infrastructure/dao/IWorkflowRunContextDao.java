package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.WorkflowRunContextPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowRunContext;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * IWorkflowRunContextDao。
 *
 * @author xiexu
 */
@Mapper
public interface IWorkflowRunContextDao extends BaseMapper<WorkflowRunContextPO> {

    int upsert(WorkflowRunContext ctx);

    WorkflowRunContext findByRunId(@Param("runId") String runId);

    int updateStatus(@Param("runId") String runId, @Param("status") String status);

    int deleteByRunIds(@Param("runIds") List<String> runIds);
}
