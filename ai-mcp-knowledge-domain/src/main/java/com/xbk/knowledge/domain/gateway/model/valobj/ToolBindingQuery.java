package com.xbk.knowledge.domain.gateway.model.valobj;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具绑定查询条件值对象。
 *
 * @author xiexu
 */
@Data
@NoArgsConstructor
public class ToolBindingQuery {

    private String bindType;
    private Long bindTargetId;

    public ToolBindingQuery(String bindType, Long bindTargetId) {
        this.bindType = bindType;
        this.bindTargetId = bindTargetId;
    }
}
