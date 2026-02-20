package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.ApprovalRequestPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ApprovalRequestPO Mapper（通过 XML 承载 SQL）。
 *
 * @author sxie
 */
@Mapper
public interface IApprovalRequestDao extends BaseMapper<ApprovalRequestPO> {

    int insertRequest(ApprovalRequestPO request);

    ApprovalRequestPO findById(@Param("id") Long id);

    ApprovalRequestPO findLatestApproved(@Param("runId") String runId,
                                       @Param("toolKey") String toolKey,
                                       @Param("now") LocalDateTime now);

    ApprovalRequestPO findLatestPending(@Param("runId") String runId,
                                      @Param("toolKey") String toolKey,
                                      @Param("now") LocalDateTime now);

    int markApproved(@Param("id") Long id,
                     @Param("approverId") Long approverId,
                     @Param("decisionComment") String decisionComment,
                     @Param("decidedAt") LocalDateTime decidedAt);

    int markRejected(@Param("id") Long id,
                     @Param("approverId") Long approverId,
                     @Param("decisionComment") String decisionComment,
                     @Param("decidedAt") LocalDateTime decidedAt);

    List<ApprovalRequestPO> list(@Param("status") String status,
                               @Param("offset") int offset,
                               @Param("pageSize") int pageSize);

    long count(@Param("status") String status);

    List<ApprovalRequestPO> listExpiredPending(@Param("now") LocalDateTime now, @Param("limit") int limit);

    int markExpired(@Param("id") Long id,
                    @Param("decisionComment") String decisionComment,
                    @Param("decidedAt") LocalDateTime decidedAt);
}
