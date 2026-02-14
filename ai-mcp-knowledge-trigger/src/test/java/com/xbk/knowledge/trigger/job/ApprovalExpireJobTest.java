package com.xbk.knowledge.trigger.job;

import com.xbk.knowledge.domain.model.entity.SysAuditEvent;
import com.xbk.knowledge.domain.model.entity.approval.ApprovalRequest;
import com.xbk.knowledge.domain.repository.AgentRunContextRepository;
import com.xbk.knowledge.domain.repository.AgentRunRepository;
import com.xbk.knowledge.domain.repository.ApprovalRequestRepository;
import com.xbk.knowledge.domain.repository.SysAuditEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 验证审批过期任务的审计写入不会触发 operator_org_id NOT NULL 约束风险。
 *
 * @author xiexu
 */
public class ApprovalExpireJobTest {

    @Test
    public void shouldWriteAuditWithNonNullOperatorOrgId() {
        ApprovalRequestRepository approvalRepo = Mockito.mock(ApprovalRequestRepository.class);
        AgentRunRepository runRepo = Mockito.mock(AgentRunRepository.class);
        AgentRunContextRepository ctxRepo = Mockito.mock(AgentRunContextRepository.class);
        SysAuditEventRepository auditRepo = Mockito.mock(SysAuditEventRepository.class);

        ApprovalExpireJob job = new ApprovalExpireJob(approvalRepo, runRepo, ctxRepo, auditRepo);

        ApprovalRequest req = ApprovalRequest.builder()
                .id(3L)
                .orgId(2L)
                .runId("r1")
                .toolKey("t1")
                .status("PENDING")
                .build();

        when(approvalRepo.listExpiredPending(any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(req))
                .thenReturn(Collections.emptyList());
        when(approvalRepo.markExpired(eq(2L), eq(3L), anyString(), any(LocalDateTime.class))).thenReturn(1);

        job.expireApprovals();

        ArgumentCaptor<SysAuditEvent> captor = ArgumentCaptor.forClass(SysAuditEvent.class);
        Mockito.verify(auditRepo).insert(captor.capture());
        SysAuditEvent event = captor.getValue();
        assertEquals(0L, event.getOperatorOrgId());
        assertEquals(2L, event.getResourceOrgId());
    }
}

