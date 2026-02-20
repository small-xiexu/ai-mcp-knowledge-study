package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.WorkflowNodeRunPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * IWorkflowNodeRunDao。
 *
 * @author sxie
 */
@Mapper
public interface IWorkflowNodeRunDao extends BaseMapper<WorkflowNodeRunPO> {

    int insertNodeRun(WorkflowNodeRunPO nodeRun);

    int updateNodeRun(WorkflowNodeRunPO nodeRun);

    WorkflowNodeRunPO findByRunIdAndNodeKey(@Param("runId") String runId,
                                          @Param("nodeKey") String nodeKey);

    List<WorkflowNodeRunPO> listByRunId(@Param("runId") String runId);

    int incrementToolCallCount(@Param("runId") String runId,
                               @Param("nodeKey") String nodeKey,
                               @Param("delta") int delta);

    int incrementToolDeniedCount(@Param("runId") String runId,
                                 @Param("nodeKey") String nodeKey,
                                 @Param("delta") int delta);

    int deleteByRunIds(@Param("runIds") List<String> runIds);
}
