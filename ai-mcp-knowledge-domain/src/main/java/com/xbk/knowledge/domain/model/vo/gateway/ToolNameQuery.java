package com.xbk.knowledge.domain.model.vo.gateway;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具名称查询条件值对象
 *
 * 职责：承载基于网关ID和工具名称的查询条件
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolNameQuery {
    /** 网关唯一标识 */
    private String gatewayId;
    /** 工具名称 */
    private String toolName;
}
