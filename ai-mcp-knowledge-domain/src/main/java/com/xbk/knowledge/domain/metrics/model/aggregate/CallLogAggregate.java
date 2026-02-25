package com.xbk.knowledge.domain.metrics.model.aggregate;

import com.xbk.knowledge.domain.metrics.model.entity.CallLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 调用日志聚合
 * 以调用日志为聚合根，保证调用记录一致性
 *
 * 职责：聚合根承载调用日志的落库一致性
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallLogAggregate {

    /**
     * 调用日志（聚合根）
     *
     * 确保调用日志的保存具有一致性边界
     */
    private CallLog callLog;
}
