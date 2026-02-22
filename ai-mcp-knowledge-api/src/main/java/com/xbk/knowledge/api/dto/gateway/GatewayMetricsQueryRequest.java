package com.xbk.knowledge.api.dto.gateway;

import com.xbk.knowledge.types.common.BaseRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Gateway 指标查询请求。
 *
 * @author sxie
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GatewayMetricsQueryRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    private String gatewayId;
    private String toolName;
    private Integer recentMinutes;
}
