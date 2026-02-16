package com.xbk.knowledge.trigger.job;

import com.xbk.knowledge.domain.repository.workflow.WorkflowNodeRunRepository;
import com.xbk.knowledge.domain.repository.workflow.WorkflowRunContextRepository;
import com.xbk.knowledge.domain.repository.workflow.WorkflowRunRepository;
import com.xbk.knowledge.types.context.OrgContext;
import com.xbk.knowledge.types.context.OrgContextHolder;
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
 
  * @author xiexu
  */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowRunCleanupJob {

    private static final int RETENTION_DAYS = 7;
    private static final int BATCH_LIMIT = 2000;
    private static final int MAX_BATCHES = 20;

    private final WorkflowRunRepository workflowRunRepository;
    private final WorkflowNodeRunRepository workflowNodeRunRepository;
    private final WorkflowRunContextRepository workflowRunContextRepository;

    /**
     * execute。
     *
     */
    @XxlJob("workflowRunCleanupHandler")
    public void execute() {
        TraceIdUtils.ensureTraceId();
        LocalDateTime cutOff = LocalDateTime.now().minusDays(RETENTION_DAYS);

        int totalDeleted = 0;
        Long orgId = OrgContextHolder.SINGLE_ORG_ID;

        OrgContext previous = OrgContextHolder.get();
        try {
            OrgContextHolder.set(new OrgContext(null, orgId, orgId, false, true));

            int batches = 0;
            while (batches++ < MAX_BATCHES) {
                List<String> runIds = workflowRunRepository.listRunIdsBefore(orgId, cutOff, BATCH_LIMIT);
                if (runIds == null || runIds.isEmpty()) {
                    break;
                }
                int d1 = workflowNodeRunRepository.deleteByRunIds(orgId, runIds);
                int d2 = workflowRunContextRepository.deleteByRunIds(orgId, runIds);
                int d3 = workflowRunRepository.deleteByRunIds(orgId, runIds);
                totalDeleted += d3;
                XxlJobHelper.log("cleanup orgId={}, runIds={}, nodeRunsDeleted={}, ctxDeleted={}, runsDeleted={}",
                        orgId, runIds.size(), d1, d2, d3);
                if (d3 <= 0) {
                    break;
                }
            }
        } finally {
            if (previous == null) {
                OrgContextHolder.clear();
            } else {
                OrgContextHolder.set(previous);
            }
        }

        XxlJobHelper.log("workflow run cleanup done. deletedRuns={}, retentionDays={}", totalDeleted, RETENTION_DAYS);
    }
}
