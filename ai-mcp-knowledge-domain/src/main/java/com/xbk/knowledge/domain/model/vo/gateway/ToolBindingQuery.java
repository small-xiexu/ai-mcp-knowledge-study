package com.xbk.knowledge.domain.model.vo.gateway;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具绑定查询条件值对象
 *
 * 职责：承载基于绑定类型和目标ID的查询条件
 * @author xiexu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolBindingQuery {
    /** 绑定类型：MODEL/SESSION */
    private String bindType;
    /** 绑定目标ID */
    private Long bindTargetId;
}
