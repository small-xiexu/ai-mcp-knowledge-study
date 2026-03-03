package com.xbk.knowledge.api.dto.gateway;

import com.xbk.knowledge.types.common.BaseRequest;
import jakarta.validation.constraints.NotBlank;
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

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 网关ID
     */
    private String gatewayId;
    /**
     * 工具名称
     */
    @NotBlank(message = "toolName 不能为空")
    private String toolName;
    /**
     * arguments
     */
    private Map<String, Object> arguments;
}
