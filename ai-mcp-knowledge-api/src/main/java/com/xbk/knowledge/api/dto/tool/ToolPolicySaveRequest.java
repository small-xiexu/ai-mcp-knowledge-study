package com.xbk.knowledge.api.dto.tool;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 工具风险策略保存请求（新增/更新）。
 
  * @author xiexu
  */
@Data
public class ToolPolicySaveRequest {

    private Long id;

    @NotBlank(message = "toolKey 不能为空")
    private String toolKey;

    /**
     * 风险等级：LOW/MEDIUM/HIGH（可空默认 MEDIUM）。
     */
    private String riskLevel;

    /**
     * 是否需要审批：true/false（可空默认 false）。
     */
    private Boolean approvalRequired;

    /**
     * 是否启用：true/false（可空默认 true）。
     */
    private Boolean enabled;

    private String remark;
}

