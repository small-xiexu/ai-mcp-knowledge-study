package com.xbk.knowledge.infrastructure.mapper.workflow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowRun;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * WorkflowRunMapper。
 *
 * @author xiexu
 */
@Mapper
public interface WorkflowRunMapper extends BaseMapper<WorkflowRun> {

    int insertRun(WorkflowRun run);

    WorkflowRun findByRunId(@Param("orgId") Long orgId, @Param("runId") String runId);

    int updateStatus(@Param("orgId") Long orgId,
                     @Param("runId") String runId,
                     @Param("status") String status,
                     @Param("errorMessage") String errorMessage,
                     @Param("endedAt") LocalDateTime endedAt);

    int updateStatusAndMetrics(WorkflowRun run);

    List<WorkflowRun> list(@Param("orgId") Long orgId,
                           @Param("status") String status,
                           @Param("offset") int offset,
                           @Param("pageSize") int pageSize);

    long count(@Param("orgId") Long orgId, @Param("status") String status);

    int deleteBefore(@Param("orgId") Long orgId,
                     @Param("cutOff") LocalDateTime cutOff,
                     @Param("limit") int limit);

    List<String> listRunIdsBefore(@Param("orgId") Long orgId,
                                  @Param("cutOff") LocalDateTime cutOff,
                                  @Param("limit") int limit);

    int deleteByRunIds(@Param("orgId") Long orgId, @Param("runIds") List<String> runIds);
}
