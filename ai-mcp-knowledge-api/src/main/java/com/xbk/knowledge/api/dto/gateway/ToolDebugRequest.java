package com.xbk.knowledge.api.dto.gateway;

import com.xbk.knowledge.types.common.BaseRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 工具调试请求。
 *
 * @author sxie
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ToolDebugRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    private String gatewayId;
    private String toolName;
    private Map<String, Object> arguments;
}
