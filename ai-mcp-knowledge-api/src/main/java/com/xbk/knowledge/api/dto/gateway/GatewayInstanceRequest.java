package com.xbk.knowledge.api.dto.gateway;

import com.xbk.knowledge.types.common.BaseRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Gateway 实例保存请求。
 *
 * @author sxie
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GatewayInstanceRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String gatewayId;
    private String gatewayName;
    private String gatewayDesc;
    private String gatewayVersion;
    private String gatewayInstructions;
    private Integer status;
}
