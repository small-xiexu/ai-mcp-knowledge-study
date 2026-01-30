package com.xbk.knowledge.domain.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型ID查询条件值对象
 * 统一承载按模型ID筛选的查询条件
 *
 * 职责：领域值对象，用于表达查询条件语义
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelIdQuery {

    /**
     * 模型ID
     */
    private Long modelId;
}
