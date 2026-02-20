package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.WorkflowNodeRunPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.workflow.model.entity.WorkflowNodeRun;
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

    int insertNodeRun(WorkflowNodeRun nodeRun);

    int updateNodeRun(WorkflowNodeRun nodeRun);

    WorkflowNodeRun findByRunIdAndNodeKey(@Param("runId") String runId,
                                          @Param("nodeKey") String nodeKey);

    List<WorkflowNodeRun> listByRunId(@Param("runId") String runId);

    int incrementToolCallCount(@Param("runId") String runId,
                               @Param("nodeKey") String nodeKey,
                               @Param("delta") int delta);

    int incrementToolDeniedCount(@Param("runId") String runId,
                                 @Param("nodeKey") String nodeKey,
                                 @Param("delta") int delta);

    int deleteByRunIds(@Param("runIds") List<String> runIds);
}
