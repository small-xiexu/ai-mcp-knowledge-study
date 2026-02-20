package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.WorkflowRunPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowRun;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * IWorkflowRunDao。
 *
 * @author sxie
 */
@Mapper
public interface IWorkflowRunDao extends BaseMapper<WorkflowRunPO> {

    int insertRun(WorkflowRun run);

    WorkflowRun findByRunId(@Param("runId") String runId);

    int updateStatus(@Param("runId") String runId,
                     @Param("status") String status,
                     @Param("errorMessage") String errorMessage,
                     @Param("endedAt") LocalDateTime endedAt);

    int updateStatusAndMetrics(WorkflowRun run);

    List<WorkflowRun> list(@Param("status") String status,
                           @Param("offset") int offset,
                           @Param("pageSize") int pageSize);

    long count(@Param("status") String status);

    int deleteBefore(@Param("cutOff") LocalDateTime cutOff,
                     @Param("limit") int limit);

    List<String> listRunIdsBefore(@Param("cutOff") LocalDateTime cutOff,
                                  @Param("limit") int limit);

    int deleteByRunIds(@Param("runIds") List<String> runIds);
}
