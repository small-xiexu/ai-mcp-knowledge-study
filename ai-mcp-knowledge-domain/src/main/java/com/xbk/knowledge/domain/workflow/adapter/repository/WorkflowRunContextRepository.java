package com.xbk.knowledge.domain.workflow.adapter.repository;

import com.xbk.knowledge.domain.workflow.model.entity.WorkflowRunContext;

import java.util.List;
import java.util.Optional;

/**
 * WorkflowRunContext 仓储接口。
 *
 * @author sxie
 */
public interface WorkflowRunContextRepository {

    /**
     * 按运行维度新增或更新上下文。
     */
    void upsert(WorkflowRunContext ctx);

    /**
     * 按运行 ID 查询记录。
     */
    Optional<WorkflowRunContext> findByRunId(String runId);

    /**
     * 更新执行状态及异常信息。
     */
    int updateStatus(String runId, String status);

    /**
     * 批量删除运行记录。
     */
    int deleteByRunIds(List<String> runIds);
}
