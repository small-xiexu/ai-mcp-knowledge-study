package com.xbk.knowledge.domain.identity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 权限分页查询条件值对象。
 *
 * 职责：领域值对象，用于承载权限查询条件。
 *
 * @author sxie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionPageQuery {

    /**
     * 资源类型关键词。
     */
    private String resourceType;

    /**
     * 动作关键词。
     */
    private String action;

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
