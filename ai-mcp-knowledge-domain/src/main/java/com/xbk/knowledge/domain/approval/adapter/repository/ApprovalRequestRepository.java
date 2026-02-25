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
     * 新增记录。
     * 
     * @param request 审批请求实体。
     * @return 已持久化的审批请求实体。
     */
    ApprovalRequest insert(ApprovalRequest request);

    /**
     * 按主键查询记录。
     * 
     * @param id 主键 ID。
     * @return 可选的审批单。
     */
    Optional<ApprovalRequest> findById(Long id);

    /**
     * 查询某 runId + toolKey 下是否存在有效的已通过审批单（未过期）。
     * 
     * @param runId 运行 ID。
     * @param toolKey 工具键。
     * @param now 当前时间（用于判断是否过期）。
     * @return 可选的审批单。
     */
    Optional<ApprovalRequest> findLatestApproved(String runId, String toolKey, LocalDateTime now);

    /**
     * 查询某 runId + toolKey 下是否存在待审批单（未过期）。
     * 
     * @param runId 运行 ID。
     * @param toolKey 工具键。
     * @param now 当前时间（用于判断是否过期）。
     * @return 可选的审批单。
     */
    Optional<ApprovalRequest> findLatestPending(String runId, String toolKey, LocalDateTime now);

    /**
     * 标记审批单为 APPROVED。
     * 
     * @param id 主键 ID。
     * @param approverId 标识 ID。
     * @param decisionComment 决策备注。
     * @param decidedAt 审批决策时间。
     * @return 影响行数。
     */
    int markApproved(Long id,
                     Long approverId,
                     String decisionComment,
                     LocalDateTime decidedAt);

    /**
     * 标记审批单为 REJECTED。
     * 
     * @param id 主键 ID。
     * @param approverId 标识 ID。
     * @param decisionComment 决策备注。
     * @param decidedAt 审批决策时间。
     * @return 影响行数。
     */
    int markRejected(Long id,
                     Long approverId,
                     String decisionComment,
                     LocalDateTime decidedAt);

    /**
     * 分页查询审批单列表（最小实现按 status 可选过滤）。
     * 
     * @param status 状态值。
     * @param offset 分页偏移量。
     * @param pageSize 分页大小。
     * @return 审批单列表。
     */
    List<ApprovalRequest> list(String status, int offset, int pageSize);

    /**
     * 统计符合条件的记录数量。
     * 
     * @param status 状态值。
     * @return 统计数量。
     */
    long count(String status);

    /**
     * 扫描全库过期的待审批单（用于定时过期处理）。
     * 
     * @param now 当前时间。
     * @param limit 批量扫描上限。
     * @return 过期的待审批单列表。
     */
    List<ApprovalRequest> listExpiredPending(LocalDateTime now, int limit);

    /**
     * 标记审批单为 EXPIRED。
     * 
     * @param id 主键 ID。
     * @param decisionComment 决策备注。
     * @param decidedAt 审批决策时间。
     * @return 影响行数。
     */
    int markExpired(Long id, String decisionComment, LocalDateTime decidedAt);

    /**
     * 删除指定 Agent 关联记录。
     * 
     * @param agentId 智能体 ID。
     * @return 影响行数。
     */
    int deleteByAgentId(Long agentId);
}
