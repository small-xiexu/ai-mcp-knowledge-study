package com.xbk.knowledge.domain.gateway.model.valobj;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网关分页查询条件值对象。
 *
 * @author sxie
 */
@Data
@NoArgsConstructor
public class GatewayPageQuery {

    private String gatewayId;
    private Integer offset;
    private Integer pageSize;

    public GatewayPageQuery(Integer offset, Integer pageSize) {
        this.offset = offset;
        this.pageSize = pageSize;
    }
}
