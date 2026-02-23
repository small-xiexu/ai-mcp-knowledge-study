package com.xbk.knowledge.api.dto.user;

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
 * 用户分页查询请求 DTO。
 *
 * 职责：接口层 DTO，用于承载用户列表查询条件。
 *
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserQueryRequest extends BaseRequest {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 用户名关键词。
     */
    private String username;

    /**
     * 用户状态。
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
