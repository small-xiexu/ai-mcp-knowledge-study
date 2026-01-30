package com.xbk.knowledge.domain.model.vo.model;

import com.xbk.knowledge.types.enums.ModelType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型类型与启用状态查询条件值对象
 * 统一承载按模型类型筛选条件
 *
 * 职责：领域值对象，用于表达查询条件语义
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelTypeEnabledQuery {

    /**
     * 模型类型
     */
    private ModelType modelType;

    /**
     * 是否启用
     */
    private Boolean enabled;
}
