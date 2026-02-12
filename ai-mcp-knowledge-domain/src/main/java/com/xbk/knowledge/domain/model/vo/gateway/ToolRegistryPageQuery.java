package com.xbk.knowledge.domain.model.vo.gateway;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具注册分页查询条件值对象
 *
 * 职责：承载工具列表分页查询条件
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolRegistryPageQuery {
    /** 所属网关ID */
    private String gatewayId;
    /** 分页偏移量 */
    private Integer offset;
    /** 每页大小 */
    private Integer pageSize;
}
