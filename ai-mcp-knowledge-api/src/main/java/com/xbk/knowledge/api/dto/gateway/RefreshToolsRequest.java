package com.xbk.knowledge.api.dto.gateway;

import lombok.Data;

/**
 * Gateway 工具刷新请求。
 *
 * @author sxie
 */
@Data
public class RefreshToolsRequest {

    /**
     * 网关 ID（不填则使用默认网关）
     */
    private String gatewayId;

    /**
     * 工具 ID（指定单个工具刷新，为空则刷新网关下所有工具）
     */
    private Long toolId;
}
