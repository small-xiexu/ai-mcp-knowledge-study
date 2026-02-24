package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.approval.model.entity.ApprovalRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.contract.PlatformContractV1;

/**
 * 审批应用服务。
 *
 * 职责：
 * 1、 提供审批单查询/审批/拒绝接口
 * 2、 支撑“方式B”：审批通过后自动继续运行产出 PlatformContractV1
 *
 * @author sxie
 */
public interface ApprovalAppService {

    PageResult<ApprovalRequest> list(String status, int offset, int pageSize);

    ApprovalRequest get(Long id);

    PlatformContractV1 approve(Long id, String decisionComment);

    ApprovalRequest reject(Long id, String decisionComment);
}

