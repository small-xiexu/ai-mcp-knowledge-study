package com.xbk.knowledge.domain.model.vo.model;

import com.xbk.knowledge.types.enums.CallStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型ID与调用状态查询条件值对象
 * 统一承载模型维度与状态维度的组合条件
 *
 * 职责：领域值对象，用于表达查询条件语义
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelIdStatusQuery {

    /**
     * 模型ID
     */
    private Long modelId;

    /**
     * 调用状态
     */
    private CallStatus status;
}
