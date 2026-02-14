package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.approval.ApprovalRequest;
import com.xbk.knowledge.domain.repository.ApprovalRequestRepository;
import com.xbk.knowledge.infrastructure.mapper.ApprovalRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * ApprovalRequest 仓储实现。
 *
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class ApprovalRequestRepositoryImpl implements ApprovalRequestRepository {

    private final ApprovalRequestMapper mapper;

    @Override
    public ApprovalRequest insert(ApprovalRequest request) {
        if (request == null) {
            return null;
        }
        if (request.getCreatedAt() == null) {
            request.setCreatedAt(LocalDateTime.now());
        }
        if (request.getUpdatedAt() == null) {
            request.setUpdatedAt(LocalDateTime.now());
        }
        mapper.insertRequest(request);
        return request;
    }

    @Override
    public Optional<ApprovalRequest> findById(Long orgId, Long id) {
        if (orgId == null || id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findById(orgId, id));
    }

    @Override
    public Optional<ApprovalRequest> findLatestApproved(Long orgId, String runId, String toolKey, LocalDateTime now) {
        if (orgId == null || !StringUtils.hasText(runId) || !StringUtils.hasText(toolKey) || now == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findLatestApproved(orgId, runId, toolKey, now));
    }

    @Override
    public Optional<ApprovalRequest> findLatestPending(Long orgId, String runId, String toolKey, LocalDateTime now) {
        if (orgId == null || !StringUtils.hasText(runId) || !StringUtils.hasText(toolKey) || now == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findLatestPending(orgId, runId, toolKey, now));
    }

    @Override
    public int markApproved(Long orgId, Long id, Long approverId, String decisionComment, LocalDateTime decidedAt) {
        if (orgId == null || id == null || decidedAt == null) {
            return 0;
        }
        return mapper.markApproved(orgId, id, approverId, decisionComment, decidedAt);
    }

    @Override
    public int markRejected(Long orgId, Long id, Long approverId, String decisionComment, LocalDateTime decidedAt) {
        if (orgId == null || id == null || decidedAt == null) {
            return 0;
        }
        return mapper.markRejected(orgId, id, approverId, decisionComment, decidedAt);
    }

    @Override
    public List<ApprovalRequest> list(Long orgId, String status, int offset, int pageSize) {
        if (orgId == null) {
            return Collections.emptyList();
        }
        int safeOffset = Math.max(offset, 0);
        int safeSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        return mapper.list(orgId, status, safeOffset, safeSize);
    }

    @Override
    public long count(Long orgId, String status) {
        if (orgId == null) {
            return 0;
        }
        return mapper.count(orgId, status);
    }

    @Override
    public List<ApprovalRequest> listExpiredPending(LocalDateTime now, int limit) {
        if (now == null) {
            return Collections.emptyList();
        }
        int safeLimit = limit <= 0 ? 200 : Math.min(limit, 1000);
        List<ApprovalRequest> list = mapper.listExpiredPending(now, safeLimit);
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public int markExpired(Long orgId, Long id, String decisionComment, LocalDateTime decidedAt) {
        if (orgId == null || id == null || decidedAt == null) {
            return 0;
        }
        return mapper.markExpired(orgId, id, decisionComment, decidedAt);
    }
}
