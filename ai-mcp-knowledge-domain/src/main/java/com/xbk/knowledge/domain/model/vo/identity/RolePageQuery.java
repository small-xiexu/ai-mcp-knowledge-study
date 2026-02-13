package com.xbk.knowledge.domain.model.vo.identity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色分页查询条件值对象。
 *
 * 职责：领域值对象，用于承载角色查询条件。
 *
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolePageQuery {

    /**
     * 角色编码关键词。
     */
    private String roleCode;

    /**
     * 状态。
     */
    private Integer status;

    /**
     * 偏移量。
     */
    private Integer offset;

    /**
     * 分页大小。
     */
    private Integer pageSize;
}
