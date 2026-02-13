package com.xbk.knowledge.domain.model.vo.identity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API Key 分页查询条件值对象。
 *
 * 职责：领域值对象，用于承载 API Key 查询条件。
 *
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyPageQuery {

    /**
     * 租户ID。
     */
    private String tenantId;

    /**
     * 归属用户ID。
     */
    private Long ownerUserId;

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
