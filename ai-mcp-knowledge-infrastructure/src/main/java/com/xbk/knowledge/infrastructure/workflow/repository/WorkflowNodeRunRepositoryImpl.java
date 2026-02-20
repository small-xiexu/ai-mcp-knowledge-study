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
 * WorkflowNodeRunRepositoryImpl。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class WorkflowNodeRunRepositoryImpl implements WorkflowNodeRunRepository {

    private final IWorkflowNodeRunDao mapper;

    /**
     * insert。
     *
     * @param nodeRun 参数
     */
    @Override
    public void insert(WorkflowNodeRun nodeRun) {
        if (nodeRun == null) {
            return;
        }
        mapper.insertNodeRun(BeanMappingUtils.map(nodeRun, WorkflowNodeRunPO.class));
    }

    /**
     * updateById。
     *
     * @param nodeRun 参数
     */
    @Override
    public void updateById(WorkflowNodeRun nodeRun) {
        if (nodeRun == null || nodeRun.getId() == null) {
            return;
        }
        mapper.updateNodeRun(BeanMappingUtils.map(nodeRun, WorkflowNodeRunPO.class));
    }

    /**
     * findByRunIdAndNodeKey。
     *
     * @param runId 参数
     * @param nodeKey 参数
     * @return 返回结果
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
     * listByRunId。
     *
     * @param runId 参数
     * @return 返回结果
     */
    @Override
    public List<WorkflowNodeRun> listByRunId(String runId) {
        if (!StringUtils.hasText(runId)) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(mapper.listByRunId(runId), WorkflowNodeRun.class);
    }

    /**
     * incrementToolCallCount。
     *
     * @param runId 参数
     * @param nodeKey 参数
     * @param delta 参数
     * @return 返回结果
     */
    @Override
    public int incrementToolCallCount(String runId, String nodeKey, int delta) {
        if (!StringUtils.hasText(runId) || !StringUtils.hasText(nodeKey)) {
            return 0;
        }
        return mapper.incrementToolCallCount(runId, nodeKey, delta);
    }

    /**
     * incrementToolDeniedCount。
     *
     * @param runId 参数
     * @param nodeKey 参数
     * @param delta 参数
     * @return 返回结果
     */
    @Override
    public int incrementToolDeniedCount(String runId, String nodeKey, int delta) {
        if (!StringUtils.hasText(runId) || !StringUtils.hasText(nodeKey)) {
            return 0;
        }
        return mapper.incrementToolDeniedCount(runId, nodeKey, delta);
    }

    /**
     * deleteByRunIds。
     *
     * @param runIds 参数
     * @return 返回结果
     */
    @Override
    public int deleteByRunIds(List<String> runIds) {
        if (runIds == null || runIds.isEmpty()) {
            return 0;
        }
        return mapper.deleteByRunIds(runIds);
    }
}
