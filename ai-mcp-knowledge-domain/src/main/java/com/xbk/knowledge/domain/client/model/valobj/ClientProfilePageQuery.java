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

    private String keyword;

    private String status;

    private Integer offset;

    private Integer pageSize;
}
