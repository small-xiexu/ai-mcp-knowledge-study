package com.xbk.knowledge.api.dto.audit;

import com.xbk.knowledge.types.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审计查询请求 DTO
 * 用于统一承载审计筛选与分页条件
 *
 * 职责：接口层 DTO，用于承载请求/响应参数并保证传输边界稳定
 * @author sxie
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditQueryRequest extends PageRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 表名（可选）
     */
    private String tableName;
}
