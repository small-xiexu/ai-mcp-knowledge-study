package com.xbk.knowledge.api.dto.workflow;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * WorkflowVersion 响应数据模型。
 *
 * @author sxie
 */
@Data
@Builder
public class WorkflowVersionResponse {

    /**
     * 主键ID
     */
    private Long id;
    /**
     * Workflow ID
     */
    private Long workflowId;
    /**
     * 版本号
     */
    private Integer versionNo;
    /**
     * 状态
     */
    private String state;
    /**
     * 变更说明
     */
    private String changeSummary;
    /**
     * graph JSON
     */
    private String graphJson;
    /**
     * 默认Config JSON
     */
    private String defaultConfigJson;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
