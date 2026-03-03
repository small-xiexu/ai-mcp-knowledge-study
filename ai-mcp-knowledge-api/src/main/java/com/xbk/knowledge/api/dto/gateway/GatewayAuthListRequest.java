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

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 网关ID
     */
    private String gatewayId;

    /**
     * api键关键字
     */
    private String apiKeyKeyword;

    /**
     * 状态
     */
    private Integer status;
}
