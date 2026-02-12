package com.xbk.knowledge.domain.model.vo.gateway;

import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class GatewayIdQuery {
    /** 网关唯一标识 */
    private String gatewayId;
}
