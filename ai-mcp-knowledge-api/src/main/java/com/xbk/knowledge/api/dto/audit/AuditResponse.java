package com.xbk.knowledge.api.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计响应 DTO
 * 用于避免直接暴露实体结构并保持响应稳定
 *
 * 职责：接口层 DTO，用于承载请求/响应参数并保证传输边界稳定
 * @author sxie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 记录ID
     */
    private Long recordId;

    /**
     * 操作类型
     */
    private String operation;

    /**
     * 变更前值
     */
    private String oldValue;

    /**
     * 变更后值
     */
    private String newValue;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
