package com.xbk.knowledge.types.context;

/**
 * 组织上下文。
 *
 * 职责：承载当前请求的组织视角（currentOrgId）与操作者所属组织（operatorOrgId）。
 *
 * 注意：
 * - operatorOrgId：操作者主组织（责任归属）
 * - currentOrgId：当前操作/查询的目标组织（资源归属）
 
  * @author xiexu
  */
public record OrgContext(
        Long operatorUserId,
        Long operatorOrgId,
        Long currentOrgId,
        boolean superAdmin,
        boolean explicitTargetOrg
) {
}
