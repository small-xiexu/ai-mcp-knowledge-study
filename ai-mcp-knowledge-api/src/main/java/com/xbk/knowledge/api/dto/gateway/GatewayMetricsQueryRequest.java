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

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 网关ID
     */
    private String gatewayId;
    /**
     * 工具名称
     */
    private String toolName;
    /**
     * recentMinutes
     */
    private Integer recentMinutes;
}
