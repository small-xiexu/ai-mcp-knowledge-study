package com.xbk.knowledge.api.dto.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Gateway 模型绑定响应 DTO。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayModelBindingResponse implements Serializable {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 模型 ID。
     */
    private Long modelId;

    /**
     * 已绑定工具 ID 列表。
     */
    private List<Long> toolIds;

    /**
     * 是否全局可见（true 表示模型可见全部启用工具）。
     */
    private Boolean globalVisible;
}
