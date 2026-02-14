package com.xbk.knowledge.domain.model.vo.common;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 启用状态查询条件值对象
 * 统一承载启用状态筛选条件
 *
 * 职责：领域值对象，用于表达查询条件语义
 * @author xiexu
 */
@Data
@NoArgsConstructor
public class EnabledQuery {

    /**
     * 组织ID（用于 org 隔离）。
     *
     * 说明：
     * - 该字段为可选，由基础设施层/应用层在进入 Mapper 前补齐当前 orgId。
     * - 保留只传 enabled 的构造函数，避免全量改动调用方。
     */
    private Long orgId;

    /**
     * 是否启用
     *
     * 为什么：统一承载启用状态筛选条件
     */
    private Boolean enabled;

    public EnabledQuery(Boolean enabled) {
        this.enabled = enabled;
    }

    public EnabledQuery(Long orgId, Boolean enabled) {
        this.orgId = orgId;
        this.enabled = enabled;
    }
}
