package com.xbk.knowledge.api.dto.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Gateway 工具映射响应 DTO。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayToolMappingResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 映射记录主键 ID。
     */
    private Long id;

    /**
     * 所属网关标识。
     */
    private String gatewayId;

    /**
     * 所属工具 ID。
     */
    private Long toolId;

    /**
     * 映射类型（request/response）。
     */
    private String mappingType;

    /**
     * 父节点 ID（根节点通常为 null）。
     */
    private Long parentId;

    /**
     * 字段名称（MCP 侧字段标识）。
     */
    private String fieldName;

    /**
     * MCP 字段类型（string/number/boolean/object/array）。
     */
    private String mcpType;

    /**
     * 字段描述说明。
     */
    private String mcpDesc;

    /**
     * 是否必填。
     */
    private Boolean isRequired;

    /**
     * 数组元素类型（当 mcpType 为 array 时使用）。
     */
    private String itemType;

    /**
     * 数组元素引用节点 ID（复杂结构时使用）。
     */
    private Long itemRefId;

    /**
     * 对应 HTTP 请求/响应中的字段路径。
     */
    private String httpPath;

    /**
     * HTTP 字段位置（body/query/path/header）。
     */
    private String httpLocation;

    /**
     * 同级节点排序值。
     */
    private Integer sortOrder;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
