package com.xbk.knowledge.domain.gateway.model.valobj;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网关 ID 查询条件值对象。
 *
 * @author sxie
 */
@Data
@NoArgsConstructor
public class GatewayIdQuery {

    /**
     * 网关 ID。
     */
    private String gatewayId;

    public GatewayIdQuery(String gatewayId) {
        this.gatewayId = gatewayId;
    }
}
