package com.xbk.knowledge.api.dto.common;

import com.xbk.knowledge.types.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * ID 查询参数
 * 用于根据 ID 查询、删除、启用、禁用等操作
 *
 * 职责：接口层 DTO，用于承载请求/响应参数并保证传输边界稳定
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class IdRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 实体 ID（必填）
     */
    @NotNull(message = "ID 不能为空")
    private Long id;
}
