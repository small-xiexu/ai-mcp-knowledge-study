package com.xbk.knowledge.domain.model.vo.gateway;

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
public class ToolBindingQuery {
    /** 组织ID */
    private Long orgId;
    /** 绑定类型：MODEL/SESSION */
    private String bindType;
    /** 绑定目标ID */
    private Long bindTargetId;

    public ToolBindingQuery(String bindType, Long bindTargetId) {
        this.bindType = bindType;
        this.bindTargetId = bindTargetId;
    }

    public ToolBindingQuery(Long orgId, String bindType, Long bindTargetId) {
        this.orgId = orgId;
        this.bindType = bindType;
        this.bindTargetId = bindTargetId;
    }
}
