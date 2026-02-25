package com.xbk.knowledge.domain.common.model.valobj;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ID 查询条件值对象。
 *
 * @author sxie
 */
@Data
@NoArgsConstructor
public class IdQuery {

    /**
     * 主键 ID。
     */
    private Long id;

    public IdQuery(Long id) {
        this.id = id;
    }
}
