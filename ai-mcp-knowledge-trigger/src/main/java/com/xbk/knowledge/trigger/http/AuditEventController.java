package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.IAuditEventService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.audit.AuditEventQueryRequest;
import com.xbk.knowledge.api.dto.audit.AuditEventResponse;
import com.xbk.knowledge.application.service.app.AuditEventAppService;
import com.xbk.knowledge.domain.audit.model.entity.SysAuditEvent;
import com.xbk.knowledge.domain.identity.model.valobj.AuditEventPageQuery;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageQueryExecutor;
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
 * @author sxie
 */
@RestController
@RequestMapping("/api/audit/events")
@RequiredArgsConstructor
public class AuditEventController implements IAuditEventService {

    /**
     * 审计事件应用服务。
     */
    private final AuditEventAppService auditEventAppService;

    /**
     * 分页查询审计事件。
     * 流程：
     * 1. 进入 Trigger 层后先执行 `audit:read` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 将请求 DTO 组装为 `AuditEventPageQuery` 查询对象。
     * 4. 调用 `auditEventAppService.queryPage` 执行分页查询。
     * 5. 将领域分页结果转换为 `AuditEventResponse` 并统一封装 `Result.success` 返回。
     * 
     * @param request 查询参数
     * @return PageResult<AuditEventResponse> 分页结果。
     */
    @SaCheckPermission("audit:read")
    @PostMapping("/list")
    @Override
    public Result<PageResult<AuditEventResponse>> list(@Valid @RequestBody AuditEventQueryRequest request) {
        return PageQueryExecutor.execute(
                request.getOffset(),
                request.getPageSize(),
                (offset, pageSize) -> auditEventAppService.queryPage(
                        new AuditEventPageQuery(
                                request.getOperatorKeyword(),
                                request.getEventType(),
                                request.getResourceType(),
                                request.getResult(),
                                offset,
                                pageSize
                        )
                ),
                this::toResponse
        );
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
