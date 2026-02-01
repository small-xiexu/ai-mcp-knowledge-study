package com.xbk.knowledge.domain.model.vo.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ID 查询条件值对象
 * 统一承载基于 ID 的查询或操作条件
 *
 * 职责：领域值对象，用于表达查询条件语义
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdQuery {

    /**
     * 实体 ID
     *
     * 为什么：统一承载基于 ID 的查询条件
     */
    private Long id;
}
