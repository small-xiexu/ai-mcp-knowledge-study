package com.xbk.knowledge.infrastructure.workflow.repository;

import com.xbk.knowledge.domain.workflow.model.entity.WorkflowNodeRun;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowNodeRunRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IWorkflowNodeRunDao;
import com.xbk.knowledge.infrastructure.dao.po.WorkflowNodeRunPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 工作流节点运行仓储实现。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class WorkflowNodeRunRepositoryImpl implements WorkflowNodeRunRepository {

    /**
     * Workflow 节点运行数据访问对象。
     */
    private final IWorkflowNodeRunDao mapper;

    /**
     * 创建并持久化工作流节点运行数据。
     *
     * @param nodeRun 节点运行记录
     */
    @Override
    public void insert(WorkflowNodeRun nodeRun) {
        if (nodeRun == null) {
            return;
        }
        mapper.insertNodeRun(BeanMappingUtils.map(nodeRun, WorkflowNodeRunPO.class));
    }

    /**
     * 更新工作流节点运行数据。
     *
     * @param nodeRun 节点运行记录
     */
    @Override
    public void updateById(WorkflowNodeRun nodeRun) {
        if (nodeRun == null || nodeRun.getId() == null) {
            return;
        }
        mapper.updateNodeRun(BeanMappingUtils.map(nodeRun, WorkflowNodeRunPO.class));
    }

    /**
     * 查询工作流节点运行。
     *
     * @param runId 运行 ID
     * @param nodeKey 节点标识
     * @return WorkflowNodeRun 查询结果（可能为空）
     */
    @Override
    public Optional<WorkflowNodeRun> findByRunIdAndNodeKey(String runId, String nodeKey) {
        if (!StringUtils.hasText(runId) || !StringUtils.hasText(nodeKey)) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findByRunIdAndNodeKey(runId, nodeKey))
                .map(item -> BeanMappingUtils.map(item, WorkflowNodeRun.class));
    }

    /**
     * 根据筛选条件查询工作流节点运行列表。
     *
     * @param runId 运行 ID
     * @return WorkflowNodeRun 列表
     */
    @Override
    public List<WorkflowNodeRun> listByRunId(String runId) {
        if (!StringUtils.hasText(runId)) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(mapper.listByRunId(runId), WorkflowNodeRun.class);
    }

    /**
     * 累加节点工具调用次数。
     *
     * @param runId 运行 ID
     * @param nodeKey 节点标识
     * @param delta 增量值
     * @return 工具调用计数累加条数
     */
    @Override
    public int incrementToolCallCount(String runId, String nodeKey, int delta) {
        if (!StringUtils.hasText(runId) || !StringUtils.hasText(nodeKey)) {
            return 0;
        }
        return mapper.incrementToolCallCount(runId, nodeKey, delta);
    }

    /**
     * 累加节点工具拒绝次数。
     *
     * @param runId 运行 ID
     * @param nodeKey 节点标识
     * @param delta 增量值
     * @return 工具拒绝计数累加条数
     */
    @Override
    public int incrementToolDeniedCount(String runId, String nodeKey, int delta) {
        if (!StringUtils.hasText(runId) || !StringUtils.hasText(nodeKey)) {
            return 0;
        }
        return mapper.incrementToolDeniedCount(runId, nodeKey, delta);
    }

    /**
     * 删除工作流节点运行数据。
     *
     * @param runIds 运行 ID 列表
     * @return 节点运行记录删除条数
     */
    @Override
    public int deleteByRunIds(List<String> runIds) {
        if (runIds == null || runIds.isEmpty()) {
            return 0;
        }
        return mapper.deleteByRunIds(runIds);
    }
}
