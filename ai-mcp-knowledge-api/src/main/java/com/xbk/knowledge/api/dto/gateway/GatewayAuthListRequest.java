package com.xbk.knowledge.api.dto.gateway;

import com.xbk.knowledge.types.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Gateway 凭证分页查询请求。
 *
 * @author sxie
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GatewayAuthListRequest extends PageRequest {

    private static final long serialVersionUID = 1L;

    private String gatewayId;
    private String apiKeyKeyword;
    private Integer status;
}
