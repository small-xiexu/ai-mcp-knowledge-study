package com.xbk.knowledge.domain.model.vo.common;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ID 查询条件值对象。
 *
 * @author xiexu
 */
@Data
@NoArgsConstructor
public class IdQuery {

    private Long id;

    public IdQuery(Long id) {
        this.id = id;
    }
}
