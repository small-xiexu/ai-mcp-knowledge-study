package com.xbk.knowledge.domain.approval.adapter.repository;

import com.xbk.knowledge.domain.approval.model.entity.ApprovalRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ApprovalRequest 仓储接口。
 *
 * 职责：承载统一审批单（高风险工具、未来可扩展发布审批等）的查询与状态变更能力。
 *
 * @author sxie
 */
public interface ApprovalRequestRepository {

    /**
     * 方法：insert。
     */
    ApprovalRequest insert(ApprovalRequest request);

    /**
     * 方法：findById。
     */
    Optional<ApprovalRequest> findById(Long id);

    /**
     * 查询某 runId + toolKey 下是否存在有效的已通过审批单（未过期）。
     */
    Optional<ApprovalRequest> findLatestApproved(String runId, String toolKey, LocalDateTime now);

    /**
     * 查询某 runId + toolKey 下是否存在待审批单（未过期）。
     */
    Optional<ApprovalRequest> findLatestPending(String runId, String toolKey, LocalDateTime now);

    /**
     * 标记审批单为 APPROVED。
     */
    int markApproved(Long id,
                     Long approverId,
                     String decisionComment,
                     LocalDateTime decidedAt);

    /**
     * 标记审批单为 REJECTED。
     */
    int markRejected(Long id,
                     Long approverId,
                     String decisionComment,
                     LocalDateTime decidedAt);

    /**
     * 分页查询审批单列表（最小实现：按 status 可选过滤）。
     */
    List<ApprovalRequest> list(String status, int offset, int pageSize);

    /**
     * 方法：count。
     */
    long count(String status);

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
    int markExpired(Long id, String decisionComment, LocalDateTime decidedAt);

    /**
     * 方法：deleteByAgentId。
     */
    int deleteByAgentId(Long agentId);
}
