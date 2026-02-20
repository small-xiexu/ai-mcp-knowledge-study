package com.xbk.knowledge.domain.model.vo.common;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 启用状态查询条件值对象。
 *
 * @author xiexu
 */
@Data
@NoArgsConstructor
public class EnabledQuery {

    private Boolean enabled;

    public EnabledQuery(Boolean enabled) {
        this.enabled = enabled;
    }
}
