package com.xbk.knowledge.trigger.job;

import com.xbk.knowledge.domain.audit.model.entity.SysAuditEvent;
import com.xbk.knowledge.domain.approval.model.entity.ApprovalRequest;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentRunContextRepository;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentRunRepository;
import com.xbk.knowledge.domain.approval.adapter.repository.ApprovalRequestRepository;
import com.xbk.knowledge.domain.audit.adapter.repository.SysAuditEventRepository;
import com.xbk.knowledge.types.trace.TraceIdUtils;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批单过期处理任务。
 *
 * 职责：定时扫描过期的 PENDING 审批单，置为 EXPIRED，并同步更新关联 run/run_context 的终态。
 *
 * @author sxie
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalExpireJob {

    private static final int BATCH_SIZE = 200;
    private static final int MAX_LOOP = 50;

    private final ApprovalRequestRepository approvalRequestRepository;
    private final AgentRunRepository agentRunRepository;
    private final AgentRunContextRepository agentRunContextRepository;
    private final SysAuditEventRepository sysAuditEventRepository;

    /**
     * XXL-Job Handler: approvalExpireHandler
     * 建议 Cron: 每 5 分钟执行一次（实际表达式以 XXL 控制台为准）。
     */
    @XxlJob("approvalExpireHandler")
    public void expireApprovals() {
        LocalDateTime now = LocalDateTime.now();
        int totalExpired = 0;

        for (int i = 0; i < MAX_LOOP; i++) {
            List<ApprovalRequest> expired = approvalRequestRepository.listExpiredPending(now, BATCH_SIZE);
            if (expired == null || expired.isEmpty()) {
                break;
            }
            for (ApprovalRequest req : expired) {
                if (req == null || req.getId() == null) {
                    continue;
                }
                try {
                    handleOne(req, now);
                    totalExpired++;
                } catch (Exception e) {
                    log.warn("处理审批单过期失败，id: {}, runId: {}", req.getId(), req.getRunId(), e);
                }
            }
        }

        log.info("审批单过期处理完成，totalExpired={}, now={}", totalExpired, now);
    }

    private void handleOne(ApprovalRequest req, LocalDateTime now) {
        Long id = req.getId();
        String runId = req.getRunId();

        int updated = approvalRequestRepository.markExpired(id, "审批超时自动过期", now);
        if (updated <= 0) {
            return;
        }

        if (StringUtils.hasText(runId)) {
            // run 终态：FAILED（可解释）
            agentRunRepository.updateStatus(runId, "FAILED", "审批超时（EXPIRED）", now);
            try {
                agentRunContextRepository.updateStatus(runId, "EXPIRED");
            } catch (Exception e) {
                log.warn("更新 agent_run_context 失败（EXPIRED），runId: {}", runId, e);
            }
        }

        recordExpireAudit(req, now);
    }

    private void recordExpireAudit(ApprovalRequest req, LocalDateTime now) {
        if (sysAuditEventRepository == null || req == null || req.getId() == null) {
            return;
        }
        String runId = req.getRunId();

        // 过期任务是平台内部流程：operator 为空，operator_type=system
        String previousTraceId = MDC.get(TraceIdUtils.TRACE_ID_KEY);
        if (StringUtils.hasText(runId)) {
            MDC.put(TraceIdUtils.TRACE_ID_KEY, runId);
        }
        try {
            SysAuditEvent event = SysAuditEvent.builder()
                    .operatorId(null)
                    .operatorType("system")
                    .eventType("TOOL_APPROVAL")
                    .resourceType("approval_request")
                    .resourceId(String.valueOf(req.getId()))
                    .action("EXPIRED")
                    .requestId(runId)
                    .result(1)
                    .errorMessage(null)
                    .costMs(0L)
                    .occurredAt(now)
                    .build();
            sysAuditEventRepository.insert(event);
        } finally {
            if (previousTraceId == null) {
                MDC.remove(TraceIdUtils.TRACE_ID_KEY);
            } else {
                MDC.put(TraceIdUtils.TRACE_ID_KEY, previousTraceId);
            }
        }
    }
}
