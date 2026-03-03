package com.xbk.knowledge.api.dto.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Gateway 工具详情响应 DTO。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayToolDetailResponse implements Serializable {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 工具主体信息。
     */
    private GatewayToolResponse tool;

    /**
     * 请求参数映射列表。
     */
    private List<GatewayToolMappingResponse> requestMappings;

    /**
     * 响应参数映射列表。
     */
    private List<GatewayToolMappingResponse> responseMappings;
}
