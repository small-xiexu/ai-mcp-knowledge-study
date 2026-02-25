package com.xbk.knowledge.domain.gateway.model.valobj;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具分页查询条件值对象。
 *
 * @author sxie
 */
@Data
@NoArgsConstructor
public class ToolRegistryPageQuery {

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

    public ToolRegistryPageQuery(String gatewayId, Integer offset, Integer pageSize) {
        this.gatewayId = gatewayId;
        this.offset = offset;
        this.pageSize = pageSize;
    }
}
