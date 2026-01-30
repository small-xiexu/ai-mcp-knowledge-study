package com.xbk.knowledge.domain.model.vo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型名称查询条件值对象
 * 统一承载模型名称查询条件
 *
 * 职责：领域值对象，用于表达查询条件语义
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelNameQuery {

    /**
     * 模型名称
     */
    private String modelName;
}
