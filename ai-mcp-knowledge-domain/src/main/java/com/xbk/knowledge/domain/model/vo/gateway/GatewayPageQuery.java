package com.xbk.knowledge.domain.model.vo.gateway;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网关分页查询条件值对象
 *
 * 职责：承载网关列表分页查询条件
 * @author xiexu
 */
@Data
@NoArgsConstructor
public class GatewayPageQuery {
    /** 组织ID */
    private Long orgId;
    /** 分页偏移量 */
    private Integer offset;
    /** 每页大小 */
    private Integer pageSize;

    public GatewayPageQuery(Integer offset, Integer pageSize) {
        this.offset = offset;
        this.pageSize = pageSize;
    }

    public GatewayPageQuery(Long orgId, Integer offset, Integer pageSize) {
        this.orgId = orgId;
        this.offset = offset;
        this.pageSize = pageSize;
    }
}
