package com.xbk.knowledge.trigger.http;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.audit.AuditEventQueryRequest;
import com.xbk.knowledge.api.dto.audit.AuditEventResponse;
import com.xbk.knowledge.application.service.app.AuditEventAppService;
import com.xbk.knowledge.domain.model.entity.SysAuditEvent;
import com.xbk.knowledge.domain.identity.model.valobj.AuditEventPageQuery;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageResultConverter;
import com.xbk.knowledge.types.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * 审计事件接口控制器。
 *
 * 职责：触发层接口适配，用于提供审计事件查询能力。
 *
 * @author xiexu
 */
@RestController
@RequestMapping("/api/audit/events")
@RequiredArgsConstructor
public class AuditEventController {

    private final AuditEventAppService auditEventAppService;

    /**
     * 分页查询审计事件。
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @SaCheckPermission("audit:read")
    @PostMapping("/list")
    public Result<PageResult<AuditEventResponse>> list(@Valid @RequestBody AuditEventQueryRequest request) {
        AuditEventPageQuery query = new AuditEventPageQuery(
                request.getOperatorKeyword(),
                request.getEventType(),
                request.getResourceType(),
                request.getResult(),
                request.getOffset(),
                request.getPageSize()
        );
        PageResult<SysAuditEvent> page = auditEventAppService.queryPage(query);
        PageResult<AuditEventResponse> responsePage = PageResultConverter.convert(page, this::toResponse);
        return Result.success(responsePage);
    }

    /**
     * 转换响应对象。
     *
     * @param event 审计事件实体
     * @return 响应 DTO
     */
    private AuditEventResponse toResponse(SysAuditEvent event) {
        return AuditEventResponse.builder()
                .id(event.getId())
                .operatorId(event.getOperatorId())
                .operatorName(event.getOperatorName())
                .operatorType(event.getOperatorType())
                .eventType(event.getEventType())
                .resourceType(event.getResourceType())
                .resourceId(event.getResourceId())
                .action(event.getAction())
                .requestId(event.getRequestId())
                .sourceIp(event.getSourceIp())
                .result(event.getResult())
                .errorMessage(event.getErrorMessage())
                .costMs(event.getCostMs())
                .occurredAt(event.getOccurredAt())
                .build();
    }

}
