package com.xbk.knowledge.infrastructure.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.domain.model.aggregate.audit.ConfigAuditAggregate;
import com.xbk.knowledge.domain.model.entity.ConfigAudit;
import com.xbk.knowledge.domain.repository.ConfigAuditRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 配置审计服务
 * 集中处理审计持久化与序列化，避免切面承担转换细节
 *
 * 职责：基础设施审计能力，用于持久化变更记录
 * @author xiexu
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuditService {

    private static final String OPERATOR_HEADER = "X-Operator";
    private static final String DEFAULT_OPERATOR = "system";

    private final ConfigAuditRepository configAuditRepository;
    private final ObjectMapper objectMapper;

    /**
     * 记录审计日志
     * 将写入流程集中在服务层，便于统一事务与序列化策略
     *
     * @param tableName 表名
     * @param recordId  记录ID
     * @param operation 操作类型
     * @param oldValue  旧值对象
     * @param newValue  新值对象
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordAudit(String tableName, Long recordId, String operation, Object oldValue, Object newValue) {
        if (recordId == null) {
            log.warn("审计日志未记录，记录ID为空，tableName: {}, operation: {}", tableName, operation);
            return;
        }

        String operator = resolveOperator();
        String oldValueJson = toJson(oldValue);
        String newValueJson = toJson(newValue);

        ConfigAudit audit = ConfigAudit.builder()
                .tableName(tableName)
                .recordId(recordId)
                .operation(operation)
                .oldValue(oldValueJson)
                .newValue(newValueJson)
                .operator(operator)
                .build();

        ConfigAuditAggregate aggregate = ConfigAuditAggregate.builder()
                .configAudit(audit)
                .build();
        configAuditRepository.save(aggregate);
        log.info("审计日志已记录，tableName: {}, recordId: {}, operation: {}, operator: {}", tableName, recordId, operation, operator);
    }

    private String resolveOperator() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            HttpServletRequest request = servletRequestAttributes.getRequest();
            String headerValue = request.getHeader(OPERATOR_HEADER);
            if (StringUtils.hasText(headerValue)) {
                return headerValue;
            }
        }
        return DEFAULT_OPERATOR;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.error("序列化审计对象失败", ex);
            return null;
        }
    }
}
