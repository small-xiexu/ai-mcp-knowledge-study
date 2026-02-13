package com.xbk.knowledge.api.dto.org;

import com.xbk.knowledge.types.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 组织查询请求 DTO。
 *
 * 职责：接口层 DTO，用于承载组织查询参数。
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrgQueryRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 状态。
     */
    private Integer status;
}
