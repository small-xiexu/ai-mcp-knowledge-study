package com.xbk.knowledge.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.approval.ApprovalRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ApprovalRequest Mapper（通过 XML 承载 SQL）。
 *
 * @author xiexu
 */
@Mapper
public interface ApprovalRequestMapper extends BaseMapper<ApprovalRequest> {

    int insertRequest(ApprovalRequest request);

    ApprovalRequest findById(@Param("orgId") Long orgId, @Param("id") Long id);

    ApprovalRequest findLatestApproved(@Param("orgId") Long orgId,
                                       @Param("runId") String runId,
                                       @Param("toolKey") String toolKey,
                                       @Param("now") LocalDateTime now);

    ApprovalRequest findLatestPending(@Param("orgId") Long orgId,
                                      @Param("runId") String runId,
                                      @Param("toolKey") String toolKey,
                                      @Param("now") LocalDateTime now);

    int markApproved(@Param("orgId") Long orgId,
                     @Param("id") Long id,
                     @Param("approverId") Long approverId,
                     @Param("decisionComment") String decisionComment,
                     @Param("decidedAt") LocalDateTime decidedAt);

    int markRejected(@Param("orgId") Long orgId,
                     @Param("id") Long id,
                     @Param("approverId") Long approverId,
                     @Param("decisionComment") String decisionComment,
                     @Param("decidedAt") LocalDateTime decidedAt);

    List<ApprovalRequest> list(@Param("orgId") Long orgId,
                               @Param("status") String status,
                               @Param("offset") int offset,
                               @Param("pageSize") int pageSize);

    long count(@Param("orgId") Long orgId, @Param("status") String status);

    List<ApprovalRequest> listExpiredPending(@Param("now") LocalDateTime now, @Param("limit") int limit);

    int markExpired(@Param("orgId") Long orgId,
                    @Param("id") Long id,
                    @Param("decisionComment") String decisionComment,
                    @Param("decidedAt") LocalDateTime decidedAt);
}
