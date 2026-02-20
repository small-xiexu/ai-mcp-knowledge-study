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

    private String gatewayId;
    private Integer offset;
    private Integer pageSize;

    public ToolRegistryPageQuery(String gatewayId, Integer offset, Integer pageSize) {
        this.gatewayId = gatewayId;
        this.offset = offset;
        this.pageSize = pageSize;
    }
}
