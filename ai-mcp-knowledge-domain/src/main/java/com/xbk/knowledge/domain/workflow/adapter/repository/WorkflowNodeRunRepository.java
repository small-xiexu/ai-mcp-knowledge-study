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
     * 
     * @param nodeRun 待新增的节点运行记录。
     */
    void insert(WorkflowNodeRun nodeRun);

    /**
     * 按主键更新记录。
     * 
     * @param nodeRun 待更新的节点运行记录。
     */
    void updateById(WorkflowNodeRun nodeRun);

    /**
     * 按运行 ID 与节点键查询节点运行记录。
     * 
     * @param runId 运行 ID。
     * @param nodeKey 节点键。
     * @return 可选的节点运行记录。
     */
    Optional<WorkflowNodeRun> findByRunIdAndNodeKey(String runId, String nodeKey);

    /**
     * 查询指定运行下的节点执行列表。
     * 
     * @param runId 运行 ID。
     * @return 节点运行记录列表。
     */
    List<WorkflowNodeRun> listByRunId(String runId);

    /**
     * 累加节点工具调用次数。
     * 
     * @param runId 运行 ID。
     * @param nodeKey 节点键。
     * @param delta 增量值。
     * @return 影响行数。
     */
    int incrementToolCallCount(String runId, String nodeKey, int delta);

    /**
     * 累加节点工具拒绝次数。
     * 
     * @param runId 运行 ID。
     * @param nodeKey 节点键。
     * @param delta 增量值。
     * @return 影响行数。
     */
    int incrementToolDeniedCount(String runId, String nodeKey, int delta);

    /**
     * 批量删除运行记录。
     * 
     * @param runIds 待删除运行 ID 列表。
     * @return 影响行数。
     */
    int deleteByRunIds(List<String> runIds);
}
