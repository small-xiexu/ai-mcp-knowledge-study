package com.xbk.knowledge.infrastructure.repository.approval;

import com.xbk.knowledge.domain.model.entity.approval.ApprovalRequest;
import com.xbk.knowledge.domain.repository.approval.ApprovalRequestRepository;
import com.xbk.knowledge.infrastructure.mapper.approval.ApprovalRequestMapper;
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

    /**
     * insert。
     *
     * @param request 参数
     * @return 返回结果
     */
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

    /**
     * findById。
     *
     * @param orgId 参数
     * @param id 参数
     * @return 返回结果
     */
    @Override
    public Optional<ApprovalRequest> findById(Long orgId, Long id) {
        if (orgId == null || id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findById(orgId, id));
    }

    /**
     * findLatestApproved。
     *
     * @param orgId 参数
     * @param runId 参数
     * @param toolKey 参数
     * @param now 参数
     * @return 返回结果
     */
    @Override
    public Optional<ApprovalRequest> findLatestApproved(Long orgId, String runId, String toolKey, LocalDateTime now) {
        if (orgId == null || !StringUtils.hasText(runId) || !StringUtils.hasText(toolKey) || now == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findLatestApproved(orgId, runId, toolKey, now));
    }

    /**
     * findLatestPending。
     *
     * @param orgId 参数
     * @param runId 参数
     * @param toolKey 参数
     * @param now 参数
     * @return 返回结果
     */
    @Override
    public Optional<ApprovalRequest> findLatestPending(Long orgId, String runId, String toolKey, LocalDateTime now) {
        if (orgId == null || !StringUtils.hasText(runId) || !StringUtils.hasText(toolKey) || now == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findLatestPending(orgId, runId, toolKey, now));
    }

    /**
     * markApproved。
     *
     * @param orgId 参数
     * @param id 参数
     * @param approverId 参数
     * @param decisionComment 参数
     * @param decidedAt 参数
     * @return 返回结果
     */
    @Override
    public int markApproved(Long orgId, Long id, Long approverId, String decisionComment, LocalDateTime decidedAt) {
        if (orgId == null || id == null || decidedAt == null) {
            return 0;
        }
        return mapper.markApproved(orgId, id, approverId, decisionComment, decidedAt);
    }

    /**
     * markRejected。
     *
     * @param orgId 参数
     * @param id 参数
     * @param approverId 参数
     * @param decisionComment 参数
     * @param decidedAt 参数
     * @return 返回结果
     */
    @Override
    public int markRejected(Long orgId, Long id, Long approverId, String decisionComment, LocalDateTime decidedAt) {
        if (orgId == null || id == null || decidedAt == null) {
            return 0;
        }
        return mapper.markRejected(orgId, id, approverId, decisionComment, decidedAt);
    }

    /**
     * list。
     *
     * @param orgId 参数
     * @param status 参数
     * @param offset 参数
     * @param pageSize 参数
     * @return 返回结果
     */
    @Override
    public List<ApprovalRequest> list(Long orgId, String status, int offset, int pageSize) {
        if (orgId == null) {
            return Collections.emptyList();
        }
        int safeOffset = Math.max(offset, 0);
        int safeSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        return mapper.list(orgId, status, safeOffset, safeSize);
    }

    /**
     * count。
     *
     * @param orgId 参数
     * @param status 参数
     * @return 返回结果
     */
    @Override
    public long count(Long orgId, String status) {
        if (orgId == null) {
            return 0;
        }
        return mapper.count(orgId, status);
    }

    /**
     * listExpiredPending。
     *
     * @param now 参数
     * @param limit 参数
     * @return 返回结果
     */
    @Override
    public List<ApprovalRequest> listExpiredPending(LocalDateTime now, int limit) {
        if (now == null) {
            return Collections.emptyList();
        }
        int safeLimit = limit <= 0 ? 200 : Math.min(limit, 1000);
        List<ApprovalRequest> list = mapper.listExpiredPending(now, safeLimit);
        return list == null ? Collections.emptyList() : list;
    }

    /**
     * markExpired。
     *
     * @param orgId 参数
     * @param id 参数
     * @param decisionComment 参数
     * @param decidedAt 参数
     * @return 返回结果
     */
    @Override
    public int markExpired(Long orgId, Long id, String decisionComment, LocalDateTime decidedAt) {
        if (orgId == null || id == null || decidedAt == null) {
            return 0;
        }
        return mapper.markExpired(orgId, id, decisionComment, decidedAt);
    }
}
