package com.xbk.knowledge.infrastructure.workflow.repository;

import com.xbk.knowledge.domain.workflow.model.entity.WorkflowRunContext;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowRunContextRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IWorkflowRunContextDao;
import com.xbk.knowledge.infrastructure.dao.po.WorkflowRunContextPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.List;

/**
 * 工作流运行上下文仓储实现。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class WorkflowRunContextRepositoryImpl implements WorkflowRunContextRepository {

    /**
     * Workflow 运行上下文数据访问对象。
     */
    private final IWorkflowRunContextDao mapper;

    /**
     * 新增或更新工作流运行上下文数据。
     *
     * @param ctx 运行上下文
     */
    @Override
    public void upsert(WorkflowRunContext ctx) {
        if (ctx == null) {
            return;
        }
        mapper.upsert(BeanMappingUtils.map(ctx, WorkflowRunContextPO.class));
    }

    /**
     * 查询工作流运行上下文。
     *
     * @param runId 运行 ID
     * @return WorkflowRunContext 查询结果（可能为空）
     */
    @Override
    public Optional<WorkflowRunContext> findByRunId(String runId) {
        if (!StringUtils.hasText(runId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findByRunId(runId))
                .map(item -> BeanMappingUtils.map(item, WorkflowRunContext.class));
    }

    /**
     * 更新工作流运行上下文数据。
     *
     * @param runId 运行 ID
     * @param status 状态值
     * @return 运行上下文状态更新条数
     */
    @Override
    public int updateStatus(String runId, String status) {
        if (!StringUtils.hasText(runId) || !StringUtils.hasText(status)) {
            return 0;
        }
        return mapper.updateStatus(runId, status);
    }

    /**
     * 删除工作流运行上下文数据。
     *
     * @param runIds 运行 ID 列表
     * @return 运行上下文删除条数
     */
    @Override
    public int deleteByRunIds(List<String> runIds) {
        if (runIds == null || runIds.isEmpty()) {
            return 0;
        }
        return mapper.deleteByRunIds(runIds);
    }
}
