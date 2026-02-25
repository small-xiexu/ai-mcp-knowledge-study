package com.xbk.knowledge.domain.agentenhancer.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AgentEnhancer 分页查询条件。
 *
 * @author sxie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentEnhancerPageQuery {

    /**
     * 关键字（模糊匹配 code/name/type，可空）。
     */
    private String keyword;

    /**
     * 启用状态（1/0，可空表示不筛选）。
     */
    private Integer enabled;

    /**
     * 类型筛选（可空）。
     */
    private String agentEnhancerType;

    /**
     * 偏移量。
     */
    private Integer offset;

    /**
     * 页大小。
     */
    private Integer pageSize;

    /**
     * 兼容原 record 访问方式。
     *
     * @return 关键字
     */
    public String keyword() {
        return keyword;
    }

    /**
     * 兼容原 record 访问方式。
     *
     * @return 启用状态
     */
    public Integer enabled() {
        return enabled;
    }

    /**
     * 兼容原 record 访问方式。
     *
     * @return 类型筛选
     */
    public String agentEnhancerType() {
        return agentEnhancerType;
    }

    /**
     * 兼容原 record 访问方式。
     *
     * @return 偏移量
     */
    public Integer offset() {
        return offset;
    }

    /**
     * 兼容原 record 访问方式。
     *
     * @return 页大小
     */
    public Integer pageSize() {
        return pageSize;
    }
}
