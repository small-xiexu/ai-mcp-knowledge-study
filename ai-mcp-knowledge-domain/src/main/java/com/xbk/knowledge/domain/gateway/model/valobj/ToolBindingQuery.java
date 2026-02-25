package com.xbk.knowledge.domain.gateway.model.valobj;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具绑定查询条件值对象。
 *
 * @author sxie
 */
@Data
@NoArgsConstructor
public class ToolBindingQuery {

    /**
     * 绑定类型。
     */
    private String bindType;

    /**
     * 绑定目标 ID。
     */
    private Long bindTargetId;

    public ToolBindingQuery(String bindType, Long bindTargetId) {
        this.bindType = bindType;
        this.bindTargetId = bindTargetId;
    }
}
