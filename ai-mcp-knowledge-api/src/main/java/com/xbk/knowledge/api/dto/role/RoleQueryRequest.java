package com.xbk.knowledge.api.dto.role;

import com.xbk.knowledge.types.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 角色分页查询请求 DTO。
 *
 * 职责：接口层 DTO，用于承载角色查询参数。
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RoleQueryRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 角色编码关键词。
     */
    private String roleCode;

    /**
     * 状态。
     */
    private Integer status;

    /**
     * 偏移量。
     */
    @NotNull(message = "offset 不能为空")
    @Min(value = 0, message = "offset 不能小于0")
    private Integer offset;

    /**
     * 分页大小。
     */
    @NotNull(message = "pageSize 不能为空")
    @Min(value = 1, message = "pageSize 不能小于1")
    @Max(value = 100, message = "pageSize 不能大于100")
    private Integer pageSize;
}
