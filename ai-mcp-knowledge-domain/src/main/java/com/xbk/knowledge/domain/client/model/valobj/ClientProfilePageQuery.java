package com.xbk.knowledge.domain.client.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 客户端画像分页查询参数。
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientProfilePageQuery {

    /**
     * 关键字。
     */
    private String keyword;

    /**
     * 状态。
     */
    private String status;

    /**
     * 分页偏移量。
     */
    private Integer offset;

    /**
     * 分页大小。
     */
    private Integer pageSize;
}
