package com.xbk.knowledge.api.dto.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Gateway 工具刷新响应 DTO。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayToolRefreshResponse implements Serializable {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 网关 ID。
     */
    private String gatewayId;

    /**
     * 刷新工具总数。
     */
    private Integer refreshedCount;

    /**
     * 刷新成功数量。
     */
    private Integer successCount;

    /**
     * 刷新失败数量。
     */
    private Integer failedCount;

    /**
     * 刷新明细列表。
     */
    private List<RefreshDetail> details;

    /**
     * 刷新明细。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshDetail implements Serializable {

        /**
         * 序列化版本号。
         */
        private static final long serialVersionUID = 1L;

        /**
         * 工具 ID。
         */
        private Long toolId;

        /**
         * 工具名称。
         */
        private String toolName;

        /**
         * HTTP 方法。
         */
        private String httpMethod;

        /**
         * HTTP URL。
         */
        private String httpUrl;

        /**
         * 连通性检测结果。
         */
        private Boolean reachable;

        /**
         * 检测消息。
         */
        private String message;

        /**
         * 失败错误信息。
         */
        private String error;
    }
}
