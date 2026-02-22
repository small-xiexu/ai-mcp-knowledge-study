package com.xbk.knowledge.api.dto.gateway;

import com.xbk.knowledge.types.common.BaseRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 模型绑定保存请求。
 *
 * @author sxie
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SaveModelBindingRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    private Long modelId;
    private List<Long> toolIds;
}
