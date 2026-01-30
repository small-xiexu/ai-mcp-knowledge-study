package com.xbk.knowledge.domain.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 启用模型ID列表查询条件值对象
 * 统一承载启用模型ID集合的查询条件
 *
 * 职责：领域值对象，用于表达查询条件语义
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnabledIdsQuery {

    /**
     * 模型ID列表
     */
    private List<Long> ids;
}
