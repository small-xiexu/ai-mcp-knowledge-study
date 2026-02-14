package com.xbk.knowledge.api.dto.tool;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工具风险策略响应 DTO。
 */
@Data
public class ToolPolicyResponse {

    private Long id;

    private Long orgId;

    private String toolKey;

    private String riskLevel;

    private Integer approvalRequired;

    private Integer enabled;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

