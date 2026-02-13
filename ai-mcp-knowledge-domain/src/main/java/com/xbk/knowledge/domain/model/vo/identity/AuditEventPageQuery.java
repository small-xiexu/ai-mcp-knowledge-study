package com.xbk.knowledge.domain.model.vo.identity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审计事件分页查询条件值对象。
 *
 * 职责：领域值对象，用于承载审计查询条件。
 *
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventPageQuery {

    /**
     * 租户ID。
     */
    private String tenantId;

    /**
     * 操作人ID。
     */
    private Long operatorId;

    /**
     * 事件类型。
     */
    private String eventType;

    /**
     * 资源类型。
     */
    private String resourceType;

    /**
     * 执行结果。
     */
    private Integer result;

    /**
     * 偏移量。
     */
    private Integer offset;

    /**
     * 分页大小。
     */
    private Integer pageSize;
}
