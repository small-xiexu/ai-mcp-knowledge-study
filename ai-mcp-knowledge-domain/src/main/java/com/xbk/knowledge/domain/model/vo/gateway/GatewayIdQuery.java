package com.xbk.knowledge.domain.model.vo.gateway;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网关 ID 查询条件值对象
 *
 * 职责：承载基于 gatewayId 的查询条件
 * @author xiexu
 */
@Data
@NoArgsConstructor
public class GatewayIdQuery {
    /** 组织ID */
    private Long orgId;
    /** 网关唯一标识 */
    private String gatewayId;

    /**
     * GatewayIdQuery。
     *
     * @param gatewayId 参数
     */
    public GatewayIdQuery(String gatewayId) {
        this.gatewayId = gatewayId;
    }

    /**
     * GatewayIdQuery。
     *
     * @param orgId 参数
     * @param gatewayId 参数
     */
    public GatewayIdQuery(Long orgId, String gatewayId) {
        this.orgId = orgId;
        this.gatewayId = gatewayId;
    }
}
