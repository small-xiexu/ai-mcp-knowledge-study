package com.xbk.knowledge.api.dto.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Gateway 实例响应 DTO。
 *
 * 职责：接口层 DTO，用于承载网关实例的对外展示字段。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayInstanceResponse implements Serializable {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * 网关 ID。
     */
    private String gatewayId;

    /**
     * 网关名称。
     */
    private String gatewayName;

    /**
     * 网关描述。
     */
    private String gatewayDesc;

    /**
     * 网关版本。
     */
    private String gatewayVersion;

    /**
     * 网关说明。
     */
    private String gatewayInstructions;

    /**
     * 状态。
     */
    private Integer status;

    /**
     * 工具数量。
     */
    private Long toolCount;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
