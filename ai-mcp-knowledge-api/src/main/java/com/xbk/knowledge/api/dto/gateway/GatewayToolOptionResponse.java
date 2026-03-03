package com.xbk.knowledge.api.dto.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Gateway 工具下拉项响应 DTO。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayToolOptionResponse implements Serializable {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 工具主键 ID。
     */
    private Long id;

    /**
     * 所属网关 ID。
     */
    private String gatewayId;

    /**
     * 工具名称。
     */
    private String toolName;

    /**
     * 工具描述。
     */
    private String toolDescription;
}
