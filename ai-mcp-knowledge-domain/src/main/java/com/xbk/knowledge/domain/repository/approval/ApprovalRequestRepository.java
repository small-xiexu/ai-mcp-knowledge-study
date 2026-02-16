package com.xbk.knowledge.domain.repository.approval;

import com.xbk.knowledge.domain.model.entity.approval.ApprovalRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ApprovalRequest 仓储接口。
 *
 * 职责：承载统一审批单（高风险工具、未来可扩展发布审批等）的查询与状态变更能力。
 *
 * @author xiexu
 */
public interface ApprovalRequestRepository {

    ApprovalRequest insert(ApprovalRequest request);

    Optional<ApprovalRequest> findById(Long orgId, Long id);

    /**
     * 查询某 runId + toolKey 下是否存在有效的已通过审批单（未过期）。
     */
    Optional<ApprovalRequest> findLatestApproved(Long orgId, String runId, String toolKey, LocalDateTime now);

    /**
     * 查询某 runId + toolKey 下是否存在待审批单（未过期）。
     */
    Optional<ApprovalRequest> findLatestPending(Long orgId, String runId, String toolKey, LocalDateTime now);

    int markApproved(Long orgId,
                     Long id,
                     Long approverId,
                     String decisionComment,
                     LocalDateTime decidedAt);

    int markRejected(Long orgId,
                     Long id,
                     Long approverId,
                     String decisionComment,
                     LocalDateTime decidedAt);

    /**
     * 分页查询审批单列表（最小实现：按 org + status 可选过滤）。
     */
    List<ApprovalRequest> list(Long orgId, String status, int offset, int pageSize);

    long count(Long orgId, String status);

    /**
     * 扫描全库过期的待审批单（用于定时过期处理）。
     *
     * @param now   当前时间
     * @param limit 批量大小
     * @return 过期待审批单列表
     */
    List<ApprovalRequest> listExpiredPending(LocalDateTime now, int limit);

    /**
     * 标记审批单为 EXPIRED。
     */
    int markExpired(Long orgId, Long id, String decisionComment, LocalDateTime decidedAt);
}
