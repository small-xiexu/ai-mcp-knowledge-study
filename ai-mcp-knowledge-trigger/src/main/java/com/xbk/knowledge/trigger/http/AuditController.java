package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageResultConverter;
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
     * 为什么：审计数据量大，必须分页避免一次性拉取全量
     * 入参：表名、分页、排序字段与顺序
     * 出参：审计日志分页结果
     */
    @PostMapping("/list")
    public Result<PageResult<AuditResponse>> listAudits(@Valid @RequestBody AuditQueryRequest request) {
        /*
         * 目的：将接口层请求转为领域查询对象，避免接口字段直接泄露到领域层
         */
        String tableName = request.getTableName();
        int offset = request.getOffset();
        Integer pageSize = request.getPageSize();
        String sortField = request.getSortField();
        String sortOrder = request.getSortOrder();
        AuditQuery query = new AuditQuery(
                tableName,
                offset,
                pageSize,
                sortField,
                sortOrder
        );
        PageResult<ConfigAudit> pageResult = auditAppService.queryAuditPage(query);

        /*
         * 目的：统一分页转换逻辑，确保输出与前端协议一致
         */
        PageResult<AuditResponse> result = PageResultConverter.convert(pageResult, this::convertToResponse);

        return Result.success(result);
    }

    /**
     * 查询所有可用表名
     *
     * 为什么：前端筛选审计表时需要下拉数据源
     * 入参：无
     * @return 表名列表
     */
    @PostMapping("/tables")
    public Result<List<String>> listTableNames() {
        List<String> tableNames = auditAppService.listTableNames();
        return Result.success(tableNames);
    }

    /**
     * 转换为响应 DTO
     *
     * 为什么：输出层只暴露必要字段，避免领域实体直接暴露
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
