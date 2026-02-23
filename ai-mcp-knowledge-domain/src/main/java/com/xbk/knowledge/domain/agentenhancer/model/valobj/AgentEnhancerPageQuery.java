package com.xbk.knowledge.domain.agentenhancer.model.valobj;

/**
 * AgentEnhancer 分页查询条件。
 *
 * @param keyword 关键字（模糊匹配 code/name/type，可空）
 * @param enabled 启用状态（1/0，可空表示不筛选）
 * @param agentEnhancerType 类型筛选（可空）
 * @param offset 偏移量
 * @param pageSize 页大小
 *
 * @author sxie
 */
public record AgentEnhancerPageQuery(String keyword,
                               Integer enabled,
                               String agentEnhancerType,
                               Integer offset,
                               Integer pageSize) {
}

