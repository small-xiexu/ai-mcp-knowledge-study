package com.xbk.knowledge.domain.identity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户分页查询条件值对象。
 *
 * 职责：领域值对象，用于承载用户检索条件。
 *
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPageQuery {

    /**
     * 用户名关键词。
     */
    private String username;

    /**
     * 用户状态。
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
