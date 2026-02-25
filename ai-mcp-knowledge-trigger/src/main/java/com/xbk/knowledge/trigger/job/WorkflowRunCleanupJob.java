package com.xbk.knowledge.trigger.job;

import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowNodeRunRepository;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowRunContextRepository;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowRunRepository;
import com.xbk.knowledge.types.trace.TraceIdUtils;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Workflow 运行明细清理任务（默认保留 7 天）。
 *
 * @author sxie
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowRunCleanupJob {

    /**
     * 运行记录保留天数。
     */
    private static final int RETENTION_DAYS = 7;

    /**
     * 单批清理运行记录上限。
     */
    private static final int BATCH_LIMIT = 2000;

    /**
     * 最大批处理次数。
     */
    private static final int MAX_BATCHES = 20;

    /**
     * Workflow 运行仓储。
     */
    private final WorkflowRunRepository workflowRunRepository;

    /**
     * Workflow 节点运行仓储。
     */
    private final WorkflowNodeRunRepository workflowNodeRunRepository;

    /**
     * Workflow 运行上下文仓储。
     */
    private final WorkflowRunContextRepository workflowRunContextRepository;

    /**
     * 执行定时任务处理。
     */
    @XxlJob("workflowRunCleanupHandler")
    public void execute() {
        TraceIdUtils.ensureTraceId();
        LocalDateTime cutOff = LocalDateTime.now().minusDays(RETENTION_DAYS);

        int totalDeleted = 0;
        int batches = 0;
        while (batches++ < MAX_BATCHES) {
            List<String> runIds = workflowRunRepository.listRunIdsBefore(cutOff, BATCH_LIMIT);
            if (runIds == null || runIds.isEmpty()) {
                break;
            }
            int d1 = workflowNodeRunRepository.deleteByRunIds(runIds);
            int d2 = workflowRunContextRepository.deleteByRunIds(runIds);
            int d3 = workflowRunRepository.deleteByRunIds(runIds);
            totalDeleted += d3;
            XxlJobHelper.log("cleanup runIds={}, nodeRunsDeleted={}, ctxDeleted={}, runsDeleted={}", runIds.size(), d1, d2, d3);
            if (d3 <= 0) {
                break;
            }
        }

        XxlJobHelper.log("workflow run cleanup done. deletedRuns={}, retentionDays={}", totalDeleted, RETENTION_DAYS);
    }
}
