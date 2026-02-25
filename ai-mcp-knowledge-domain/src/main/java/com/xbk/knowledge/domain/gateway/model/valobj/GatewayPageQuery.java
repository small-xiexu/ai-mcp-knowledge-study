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

    /**
     * 网关 ID。
     */
    private String gatewayId;

    /**
     * 分页偏移量。
     */
    private Integer offset;

    /**
     * 分页大小。
     */
    private Integer pageSize;

    public GatewayPageQuery(Integer offset, Integer pageSize) {
        this.offset = offset;
        this.pageSize = pageSize;
    }
}
