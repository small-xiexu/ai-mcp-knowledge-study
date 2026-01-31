package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.api.dto.audit.AuditQueryRequest;
import com.xbk.knowledge.api.dto.audit.AuditResponse;
import com.xbk.knowledge.domain.model.entity.ConfigAudit;
import com.xbk.knowledge.domain.model.vo.audit.AuditQuery;
import com.xbk.knowledge.application.service.app.AuditAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;
import java.util.function.Function;


/**
 * 审计日志查询 Controller
 * 负责接收 HTTP 请求，调用应用服务，转换响应
 *
 * 职责：HTTP 接口适配，用于转发应用层能力
 * @author xiexu
 */
@Slf4j
@RestController
@RequestMapping("/api/audits")
@RequiredArgsConstructor
public class AuditController {

    private final AuditAppService auditAppService;

    /**
     * 分页查询审计日志
     *
     * @param request 查询参数
     * @return 审计日志分页结果
     */
    @PostMapping("/list")
    public Result<PageResult<AuditResponse>> listAudits(@Valid @RequestBody AuditQueryRequest request) {
        // 调用应用服务查询
        String tableName = request.getTableName();
        Long recordId = request.getRecordId();
        String operator = request.getOperator();
        int offset = request.getOffset();
        Integer pageSize = request.getPageSize();
        String sortField = request.getSortField();
        String sortOrder = request.getSortOrder();
        AuditQuery query = new AuditQuery(
                tableName,
                recordId,
                operator,
                offset,
                pageSize,
                sortField,
                sortOrder
        );
        PageResult<ConfigAudit> pageResult = auditAppService.queryAuditPage(query);

        // 转换为响应 DTO
        Function<ConfigAudit, AuditResponse> responseConverter = this::convertToResponse;
        List<AuditResponse> records = pageResult
                .getRecords()
                .stream()
                .map(responseConverter)
                .toList();

        // 构建分页结果
        Long total = pageResult.getTotal();
        Integer pageNum = pageResult.getPageNum();
        Integer resultPageSize = pageResult.getPageSize();
        PageResult<AuditResponse> result = PageResult.of(
                records,
                total,
                pageNum,
                resultPageSize
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
