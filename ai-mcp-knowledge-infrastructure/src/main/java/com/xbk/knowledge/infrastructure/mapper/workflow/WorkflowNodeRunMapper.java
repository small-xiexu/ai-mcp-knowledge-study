package com.xbk.knowledge.infrastructure.mapper.workflow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.workflow.WorkflowNodeRun;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * WorkflowNodeRunMapper。
 *
 * @author xiexu
 */
@Mapper
public interface WorkflowNodeRunMapper extends BaseMapper<WorkflowNodeRun> {

    int insertNodeRun(WorkflowNodeRun nodeRun);

    int updateNodeRun(WorkflowNodeRun nodeRun);

    WorkflowNodeRun findByRunIdAndNodeKey(@Param("orgId") Long orgId,
                                          @Param("runId") String runId,
                                          @Param("nodeKey") String nodeKey);

    List<WorkflowNodeRun> listByRunId(@Param("orgId") Long orgId, @Param("runId") String runId);

    int incrementToolCallCount(@Param("orgId") Long orgId,
                               @Param("runId") String runId,
                               @Param("nodeKey") String nodeKey,
                               @Param("delta") int delta);

    int incrementToolDeniedCount(@Param("orgId") Long orgId,
                                 @Param("runId") String runId,
                                 @Param("nodeKey") String nodeKey,
                                 @Param("delta") int delta);

    int deleteByRunIds(@Param("orgId") Long orgId, @Param("runIds") List<String> runIds);
}
