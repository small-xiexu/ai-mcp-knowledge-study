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

    private static final long serialVersionUID = 1L;

    private Long id;
    private String gatewayId;
    private String apiKey;
    private Integer rateLimit;
    private LocalDateTime expireTime;
    private Integer status;
}
