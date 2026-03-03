package com.xbk.knowledge.api.dto.gateway;

import com.xbk.knowledge.types.common.BaseRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模型绑定查询请求。
 *
 * @author sxie
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModelBindingQueryRequest extends BaseRequest {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 模型ID
     */
    @NotNull(message = "modelId 不能为空")
    private Long modelId;
}
