package com.xbk.knowledge.infrastructure.approval.repository;

import com.xbk.knowledge.domain.approval.model.entity.ApprovalRequest;
import com.xbk.knowledge.domain.approval.adapter.repository.ApprovalRequestRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IApprovalRequestDao;
import com.xbk.knowledge.infrastructure.dao.po.ApprovalRequestPO;
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
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class ApprovalRequestRepositoryImpl implements ApprovalRequestRepository {

    private final IApprovalRequestDao mapper;

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
        mapper.insertRequest(BeanMappingUtils.map(request, ApprovalRequestPO.class));
        return request;
    }

    /**
     * findById。
     *
     * @param id 参数
     * @return 返回结果
     */
    @Override
    public Optional<ApprovalRequest> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findById(id))
                .map(item -> BeanMappingUtils.map(item, ApprovalRequest.class));
    }

    /**
     * findLatestApproved。
     *
     * @param runId 参数
     * @param toolKey 参数
     * @param now 参数
     * @return 返回结果
     */
    @Override
    public Optional<ApprovalRequest> findLatestApproved(String runId, String toolKey, LocalDateTime now) {
        if (!StringUtils.hasText(runId) || !StringUtils.hasText(toolKey) || now == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findLatestApproved(runId, toolKey, now))
                .map(item -> BeanMappingUtils.map(item, ApprovalRequest.class));
    }

    /**
     * findLatestPending。
     *
     * @param runId 参数
     * @param toolKey 参数
     * @param now 参数
     * @return 返回结果
     */
    @Override
    public Optional<ApprovalRequest> findLatestPending(String runId, String toolKey, LocalDateTime now) {
        if (!StringUtils.hasText(runId) || !StringUtils.hasText(toolKey) || now == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findLatestPending(runId, toolKey, now))
                .map(item -> BeanMappingUtils.map(item, ApprovalRequest.class));
    }

    /**
     * markApproved。
     *
     * @param id 参数
     * @param approverId 参数
     * @param decisionComment 参数
     * @param decidedAt 参数
     * @return 返回结果
     */
    @Override
    public int markApproved(Long id, Long approverId, String decisionComment, LocalDateTime decidedAt) {
        if (id == null || decidedAt == null) {
            return 0;
        }
        return mapper.markApproved(id, approverId, decisionComment, decidedAt);
    }

    /**
     * markRejected。
     *
     * @param id 参数
     * @param approverId 参数
     * @param decisionComment 参数
     * @param decidedAt 参数
     * @return 返回结果
     */
    @Override
    public int markRejected(Long id, Long approverId, String decisionComment, LocalDateTime decidedAt) {
        if (id == null || decidedAt == null) {
            return 0;
        }
        return mapper.markRejected(id, approverId, decisionComment, decidedAt);
    }

    /**
     * list。
     *
     * @param status 参数
     * @param offset 参数
     * @param pageSize 参数
     * @return 返回结果
     */
    @Override
    public List<ApprovalRequest> list(String status, int offset, int pageSize) {
        int safeOffset = Math.max(offset, 0);
        int safeSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        return BeanMappingUtils.mapList(mapper.list(status, safeOffset, safeSize), ApprovalRequest.class);
    }

    /**
     * count。
     *
     * @param status 参数
     * @return 返回结果
     */
    @Override
    public long count(String status) {
        return mapper.count(status);
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
        List<ApprovalRequestPO> list = mapper.listExpiredPending(now, safeLimit);
        return list == null ? Collections.emptyList() : BeanMappingUtils.mapList(list, ApprovalRequest.class);
    }

    /**
     * markExpired。
     *
     * @param id 参数
     * @param decisionComment 参数
     * @param decidedAt 参数
     * @return 返回结果
     */
    @Override
    public int markExpired(Long id, String decisionComment, LocalDateTime decidedAt) {
        if (id == null || decidedAt == null) {
            return 0;
        }
        return mapper.markExpired(id, decisionComment, decidedAt);
    }
}
