package com.xbk.knowledge.domain.model.vo.identity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 组织查询条件值对象。
 *
 * 职责：领域值对象，用于承载组织查询条件。
 *
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrgQuery {

    /**
     * 状态。
     */
    private Integer status;
}
