package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.types.common.PageRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.api.dto.AuditQueryRequest;
import com.xbk.knowledge.api.dto.AuditResponse;
import com.xbk.knowledge.domain.model.entity.ConfigAudit;
import com.xbk.knowledge.domain.repository.ConfigAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审计日志查询 Controller
 * 统一审计查询入口，避免多处重复查询逻辑
 *
 * @author xiexu
 */
@Slf4j
@RestController
@RequestMapping("/api/audits")
@RequiredArgsConstructor
public class AuditController {

    private final ConfigAuditRepository configAuditRepository;

    /**
     * 分页查询审计日志
     *
     * @param request 查询参数
     * @return 审计日志分页结果
     */
    @GetMapping
    public Result<PageResult<AuditResponse>> listAudits(AuditQueryRequest request) {
        log.info("查询审计日志，tableName: {}, recordId: {}, operator: {}", request.tableName(), request.recordId(), request.operator());

        var pageRequest = request.toPageRequest();
        pageRequest.validate();

        var sortField = pageRequest.getSortField();
        var sortDirection = Sort.Direction.fromString(pageRequest.getSortOrder());
        var sort = (sortField == null || sortField.isBlank())
                ? Sort.by(sortDirection, "createdAt")
                : Sort.by(sortDirection, sortField);

        var pageable = org.springframework.data.domain.PageRequest.of(
                pageRequest.getPageNum() - 1,
                pageRequest.getPageSize(),
                sort
        );

        var tableName = normalizeText(request.tableName());
        var operator = normalizeText(request.operator());

        var page = configAuditRepository.findByConditions(tableName, request.recordId(), operator, pageable);
        var records = page.getContent().stream()
                .map(this::convertToResponse)
                .toList();

        var pageResult = PageResult.of(records, page.getTotalElements(), pageRequest.getPageNum(), pageRequest.getPageSize());
        return Result.success(pageResult);
    }

    private AuditResponse convertToResponse(ConfigAudit audit) {
        return new AuditResponse(
                audit.getId(),
                audit.getTableName(),
                audit.getRecordId(),
                audit.getOperation(),
                audit.getOldValue(),
                audit.getNewValue(),
                audit.getOperator(),
                audit.getCreatedAt()
        );
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
