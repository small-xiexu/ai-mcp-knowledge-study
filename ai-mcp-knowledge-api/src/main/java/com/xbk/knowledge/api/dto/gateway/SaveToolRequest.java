package com.xbk.knowledge.api.dto.gateway;

import com.xbk.knowledge.types.common.BaseRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 工具保存请求。
 *
 * @author sxie
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SaveToolRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String gatewayId;
    private String toolName;
    private String toolDescription;
    private String httpUrl;
    private String httpMethod;
    private String httpHeaders;
    private Integer timeout;
    private Integer retryTimes;
    private Integer status;
    private List<MappingNodeRequest> requestMappings;
    private List<MappingNodeRequest> responseMappings;
}
