package com.xbk.knowledge.api.dto.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Gateway 工具调试响应 DTO。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayToolDebugResponse implements Serializable {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 调用是否成功。
     */
    private boolean success;

    /**
     * 调用结果内容。
     */
    private String content;

    /**
     * 错误码（失败时返回）。
     */
    private String errorCode;
}
