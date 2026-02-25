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

    /**
     * 审批请求数据访问对象。
     */
    private final IApprovalRequestDao mapper;

    /**
     * 创建并持久化审批请求数据。
     * 
     * @param request 审批请求创建参数。
     * @return ApprovalRequest 数据。
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
        ApprovalRequestPO po = BeanMappingUtils.map(request, ApprovalRequestPO.class);
        mapper.insertRequest(po);
        request.setId(po == null ? null : po.getId());
        return request;
    }

    /**
     * 查询审批请求。
     * 
     * @param id 主键 ID
     * @return ApprovalRequest 查询结果（可能为空）。
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
     * 查询审批请求。
     * 
     * @param runId 运行 ID
     * @param toolKey 工具标识
     * @param now 当前时间
     * @return ApprovalRequest 查询结果（可能为空）。
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
     * 查询审批请求。
     * 
     * @param runId 运行 ID
     * @param toolKey 工具标识
     * @param now 当前时间
     * @return ApprovalRequest 查询结果（可能为空）。
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
     * 标记业务状态。
     * 
     * @param id 主键 ID
     * @param approverId 审批人 ID。
     * @param decisionComment 审批意见。
     * @param decidedAt 审批时间。
     * @return 审批通过更新条数。
     */
    @Override
    public int markApproved(Long id, Long approverId, String decisionComment, LocalDateTime decidedAt) {
        if (id == null || decidedAt == null) {
            return 0;
        }
        return mapper.markApproved(id, approverId, decisionComment, decidedAt);
    }

    /**
     * 标记业务状态。
     * 
     * @param id 主键 ID
     * @param approverId 审批人 ID。
     * @param decisionComment 审批意见。
     * @param decidedAt 审批时间。
     * @return 审批拒绝更新条数。
     */
    @Override
    public int markRejected(Long id, Long approverId, String decisionComment, LocalDateTime decidedAt) {
        if (id == null || decidedAt == null) {
            return 0;
        }
        return mapper.markRejected(id, approverId, decisionComment, decidedAt);
    }

    /**
     * 根据筛选条件查询审批请求列表。
     * 
     * @param status 状态值
     * @param offset 分页偏移量
     * @param pageSize 分页大小
     * @return ApprovalRequest 列表数据。
     */
    @Override
    public List<ApprovalRequest> list(String status, int offset, int pageSize) {
        int safeOffset = Math.max(offset, 0);
        int safeSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        return BeanMappingUtils.mapList(mapper.list(status, safeOffset, safeSize), ApprovalRequest.class);
    }

    /**
     * 按条件统计业务数量。
     * 
     * @param status 状态值
     * @return 统计数量
     */
    @Override
    public long count(String status) {
        return mapper.count(status);
    }

    /**
     * 根据筛选条件查询审批请求列表。
     * 
     * @param now 当前时间
     * @param limit 限制数量
     * @return ApprovalRequest 列表数据。
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
     * 标记业务状态。
     * 
     * @param id 主键 ID
     * @param decisionComment 审批意见。
     * @param decidedAt 审批时间。
     * @return 审批过期更新条数。
     */
    @Override
    public int markExpired(Long id, String decisionComment, LocalDateTime decidedAt) {
        if (id == null || decidedAt == null) {
            return 0;
        }
        return mapper.markExpired(id, decisionComment, decidedAt);
    }

    /**
     * 删除审批请求数据。
     * 
     * @param agentId Agent ID
     * @return 审批记录删除条数。
     */
    @Override
    public int deleteByAgentId(Long agentId) {
        if (agentId == null) {
            return 0;
        }
        return mapper.deleteByAgentId(agentId);
    }
}
