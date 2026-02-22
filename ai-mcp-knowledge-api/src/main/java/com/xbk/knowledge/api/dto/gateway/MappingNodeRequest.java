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

    private Long parentId;
    private String fieldName;
    private String mcpType;
    private String mcpDesc;
    private Boolean isRequired;
    private String itemType;
    private Long itemRefId;
    private String httpPath;
    private String httpLocation;
    private Integer sortOrder;
    private List<MappingNodeRequest> children;
}
