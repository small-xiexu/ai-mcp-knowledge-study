package com.xbk.knowledge.api.dto.gateway;

import com.xbk.knowledge.types.common.BaseRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Gateway 凭证保存请求。
 *
 * @author sxie
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SaveGatewayAuthRequest extends BaseRequest {

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
     * API Key
     */
    private String apiKey;
    /**
     * rateLimit
     */
    private Integer rateLimit;
    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
    /**
     * 状态
     */
    private Integer status;
}
