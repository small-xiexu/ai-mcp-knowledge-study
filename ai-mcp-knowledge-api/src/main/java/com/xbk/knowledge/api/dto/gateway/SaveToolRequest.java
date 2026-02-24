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

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;
    /**
     * 网关ID
     */
    private String gatewayId;
    /**
     * 工具名称
     */
    private String toolName;
    /**
     * 工具描述
     */
    private String toolDescription;
    /**
     * HTTP地址
     */
    private String httpUrl;
    /**
     * HTTP方法
     */
    private String httpMethod;
    /**
     * HTTP 请求头
     */
    private String httpHeaders;
    /**
     * 超时时间
     */
    private Integer timeout;
    /**
     * 重试次数
     */
    private Integer retryTimes;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 请求Mappings
     */
    private List<MappingNodeRequest> requestMappings;
    /**
     * 响应Mappings
     */
    private List<MappingNodeRequest> responseMappings;
}
