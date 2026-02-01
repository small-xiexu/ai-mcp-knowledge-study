package com.xbk.knowledge.domain.model.vo.metrics;

import com.xbk.knowledge.types.enums.CallStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 调用状态查询条件值对象
 * 统一承载按调用状态筛选条件
 *
 * 职责：领域值对象，用于表达查询条件语义
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallStatusQuery {

    /**
     * 调用状态
     *
     * 为什么：按成功/失败状态过滤
     */
    private CallStatus status;
}
