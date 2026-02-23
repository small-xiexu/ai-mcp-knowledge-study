package com.xbk.knowledge.domain.workflow.adapter.repository;

import com.xbk.knowledge.domain.workflow.model.entity.WorkflowNodeRun;

import java.util.List;
import java.util.Optional;

/**
 * WorkflowNodeRun 仓储接口。
 *
 * @author sxie
 */
public interface WorkflowNodeRunRepository {

    /**
     * 新增记录。
     */
    void insert(WorkflowNodeRun nodeRun);

    /**
     * 按主键更新记录。
     */
    void updateById(WorkflowNodeRun nodeRun);

    /**
     * 按运行 ID 与节点键查询节点运行记录。
     */
    Optional<WorkflowNodeRun> findByRunIdAndNodeKey(String runId, String nodeKey);

    /**
     * 查询指定运行下的节点执行列表。
     */
    List<WorkflowNodeRun> listByRunId(String runId);

    /**
     * 累加节点工具调用次数。
     */
    int incrementToolCallCount(String runId, String nodeKey, int delta);

    /**
     * 累加节点工具拒绝次数。
     */
    int incrementToolDeniedCount(String runId, String nodeKey, int delta);

    /**
     * 批量删除运行记录。
     */
    int deleteByRunIds(List<String> runIds);
}
