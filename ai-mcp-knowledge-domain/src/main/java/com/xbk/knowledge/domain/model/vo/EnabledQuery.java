package com.xbk.knowledge.domain.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 启用状态查询条件值对象
 * 统一承载启用状态筛选条件
 *
 * 职责：领域值对象，用于表达查询条件语义
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnabledQuery {

    /**
     * 是否启用
     */
    private Boolean enabled;
}
