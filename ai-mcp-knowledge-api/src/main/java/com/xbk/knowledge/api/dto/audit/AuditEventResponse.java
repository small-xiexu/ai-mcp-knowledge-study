package com.xbk.knowledge.api.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计事件响应 DTO。
 *
 * 职责：接口层 DTO，用于输出审计事件信息。
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 审计ID。
     */
    private Long id;

    /**
     * 操作人ID。
     */
    private Long operatorId;

    /**
     * 操作人用户名。
     */
    private String operatorName;

    /**
     * 操作主体类型。
     */
    private String operatorType;

    /**
     * 事件类型。
     */
    private String eventType;

    /**
     * 资源类型。
     */
    private String resourceType;

    /**
     * 资源ID。
     */
    private String resourceId;

    /**
     * 动作。
     */
    private String action;

    /**
     * 请求ID。
     */
    private String requestId;

    /**
     * 来源IP。
     */
    private String sourceIp;

    /**
     * 执行结果。
     */
    private Integer result;

    /**
     * 错误信息。
     */
    private String errorMessage;

    /**
     * 耗时毫秒。
     */
    private Long costMs;

    /**
     * 发生时间。
     */
    private LocalDateTime occurredAt;
}
