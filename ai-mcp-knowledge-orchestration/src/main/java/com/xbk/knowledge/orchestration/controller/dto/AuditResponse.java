package com.xbk.knowledge.orchestration.controller.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计响应 DTO
 * 用于避免直接暴露实体结构并保持响应稳定
 *
 * @author xiexu
 */
public record AuditResponse(
        Long id,
        String tableName,
        Long recordId,
        String operation,
        String oldValue,
        String newValue,
        String operator,
        LocalDateTime createdAt
) implements Serializable {
}
