package com.xbk.knowledge.domain.common.model.valobj;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 启用状态查询条件值对象。
 *
 * @author sxie
 */
@Data
@NoArgsConstructor
public class EnabledQuery {

    private Boolean enabled;

    public EnabledQuery(Boolean enabled) {
        this.enabled = enabled;
    }
}
