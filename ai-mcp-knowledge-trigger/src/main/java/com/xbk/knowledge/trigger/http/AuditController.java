package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.api.dto.audit.AuditQueryRequest;
import com.xbk.knowledge.api.dto.audit.AuditResponse;
import com.xbk.knowledge.domain.model.entity.ConfigAudit;
import com.xbk.knowledge.domain.service.IAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;


/**
 * 审计日志查询 Controller
 * 负责接收 HTTP 请求，调用领域服务，转换响应
 *
 * 职责：HTTP 接口适配，用于转发应用层能力
 * @author xiexu
 */
@Slf4j
@RestController
@RequestMapping("/api/audits")
@RequiredArgsConstructor
public class AuditController {

    private final IAuditService auditService;

    /**
     * 分页查询审计日志
     *
     * @param request 查询参数
     * @return 审计日志分页结果
     */
    @PostMapping("/list")
    public Result<PageResult<AuditResponse>> listAudits(@Valid @RequestBody AuditQueryRequest request) {
        // 验证并修正分页参数
        request.validate();

        // 调用领域服务查询
        PageResult<ConfigAudit> pageResult = auditService.queryAuditPage(
                request.getTableName(),
                request.getRecordId(),
                request.getOperator(),
                request.getOffset(),
                request.getPageSize(),
                request.getSortField(),
                request.getSortOrder()
        );

        // 转换为响应 DTO
        List<AuditResponse> records = pageResult.getRecords().stream()
                .map(this::convertToResponse)
                .toList();

        // 构建分页结果
        PageResult<AuditResponse> result = PageResult.of(
                records,
                pageResult.getTotal(),
                pageResult.getPageNum(),
                pageResult.getPageSize()
        );

        return Result.success(result);
    }

    /**
     * 转换为响应 DTO
     */
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
}
