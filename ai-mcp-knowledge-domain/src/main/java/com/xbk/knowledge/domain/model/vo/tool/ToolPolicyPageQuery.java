package com.xbk.knowledge.domain.model.vo.tool;

/**
 * ToolPolicy 分页查询条件。
 *
 * @param orgId 组织ID
 * @param keyword 关键字（模糊匹配 toolKey，可空）
 * @param enabled 启用状态（1/0，可空表示不筛选）
 * @param offset 偏移量
 * @param pageSize 页大小
 */
public record ToolPolicyPageQuery(Long orgId,
                                 String keyword,
                                 Integer enabled,
                                 Integer offset,
                                 Integer pageSize) {
}

