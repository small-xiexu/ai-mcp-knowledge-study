package com.xbk.knowledge.api.dto.gateway;

import com.xbk.knowledge.types.enums.ModelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Gateway 模型下拉项响应 DTO。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayModelOptionResponse implements Serializable {

    /**
     * 序列化版本号。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 模型主键 ID。
     */
    private Long id;

    /**
     * 模型名称。
     */
    private String modelName;

    /**
     * 模型类型（OPENAI/ANTHROPIC/GEMINI 等）。
     */
    private ModelType modelType;
}
