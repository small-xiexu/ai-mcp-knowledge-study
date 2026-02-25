package com.xbk.knowledge.domain.llm.model.valobj;

import com.xbk.knowledge.types.enums.CallStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型ID与调用状态查询条件值对象
 * 统一承载模型维度与状态维度的组合条件
 *
 * 职责：领域值对象，用于表达查询条件语义
 * @author sxie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelIdStatusQuery {

    /**
     * 模型ID
     *
     * 用于按模型维度筛选
     */
    private Long modelId;

    /**
     * 调用状态
     *
     * 用于按状态维度筛选
     */
    private CallStatus status;
}
