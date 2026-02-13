package com.xbk.knowledge.api.dto.audit;

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
 * 审计事件分页查询请求 DTO。
 *
 * 职责：接口层 DTO，用于承载审计事件查询参数。
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuditEventQueryRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 租户ID（仅超管可指定）。
     */
    private String tenantId;

    /**
     * 操作人ID。
     */
    private Long operatorId;

    /**
     * 事件类型。
     */
    private String eventType;

    /**
     * 资源类型。
     */
    private String resourceType;

    /**
     * 执行结果。
     */
    private Integer result;

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
