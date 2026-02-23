package com.xbk.knowledge.api.dto.gateway;

import lombok.Data;

import java.util.List;

/**
 * 工具映射节点请求。
 *
 * @author sxie
 */
@Data
public class MappingNodeRequest {

    /**
     * parent ID
     */
    private Long parentId;
    /**
     * field名称
     */
    private String fieldName;
    /**
     * MCP类型
     */
    private String mcpType;
    /**
     * MCP描述
     */
    private String mcpDesc;
    /**
     * 是否required
     */
    private Boolean isRequired;
    /**
     * item类型
     */
    private String itemType;
    /**
     * itemRef ID
     */
    private Long itemRefId;
    /**
     * HTTP路径
     */
    private String httpPath;
    /**
     * HTTPLocation
     */
    private String httpLocation;
    /**
     * sort顺序
     */
    private Integer sortOrder;
    /**
     * children
     */
    private List<MappingNodeRequest> children;
}
