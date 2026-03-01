package com.xbk.knowledge.api.dto.gateway;

import com.xbk.knowledge.types.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Gateway 工具分页查询请求。
 *
 * @author sxie
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ToolListRequest extends PageRequest {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 网关 ID
     */
    private String gatewayId;

    /**
     * 工具名称关键词（支持模糊搜索）
     */
    private String toolNameKeyword;

    /**
     * 工具描述关键词（支持模糊搜索）
     */
    private String toolDescriptionKeyword;

    /**
     * 工具状态筛选（0-禁用，1-启用，null-全部）
     */
    private Integer status;
}
