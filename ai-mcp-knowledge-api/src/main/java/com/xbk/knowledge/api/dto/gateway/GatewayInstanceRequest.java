package com.xbk.knowledge.api.dto.gateway;

import com.xbk.knowledge.types.common.BaseRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Gateway 实例保存请求。
 *
 * @author sxie
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GatewayInstanceRequest extends BaseRequest {

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
     * 网关名称
     */
    @NotBlank(message = "gatewayName 不能为空")
    private String gatewayName;
    /**
     * 网关描述
     */
    private String gatewayDesc;
    /**
     * 网关版本
     */
    private String gatewayVersion;
    /**
     * 网关说明
     */
    private String gatewayInstructions;
    /**
     * 状态
     */
    private Integer status;
}
