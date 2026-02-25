package com.xbk.knowledge.api.dto.model;

import com.xbk.knowledge.types.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模型配置查询请求
 * 用于分页查询模型配置列表
 *
 * 职责：接口层 DTO，用于承载请求/响应参数并保证传输边界稳定
 * @author sxie
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModelConfigQueryRequest extends PageRequest {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    // 可以在这里添加额外的查询条件，如
    // private Boolean enabled; // 是否启用
    // private ModelType modelType; // 模型类型
}
