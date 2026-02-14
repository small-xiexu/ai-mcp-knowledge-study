package com.xbk.knowledge.domain.model.vo.common;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ID 查询条件值对象
 * 统一承载基于 ID 的查询或操作条件
 *
 * 职责：领域值对象，用于表达查询条件语义
 * @author xiexu
 */
@Data
@NoArgsConstructor
public class IdQuery {

    /**
     * 组织ID（用于 org 隔离）。
     *
     * 说明：
     * - 该字段为可选，由基础设施层/应用层在进入 Mapper 前补齐当前 orgId。
     * - 保留只传 id 的构造函数，避免全量改动调用方。
     */
    private Long orgId;

    /**
     * 实体 ID
     *
     * 为什么：统一承载基于 ID 的查询条件
     */
    private Long id;

    public IdQuery(Long id) {
        this.id = id;
    }

    public IdQuery(Long orgId, Long id) {
        this.orgId = orgId;
        this.id = id;
    }
}
