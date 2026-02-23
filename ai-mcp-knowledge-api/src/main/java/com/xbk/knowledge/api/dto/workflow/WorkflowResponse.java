package com.xbk.knowledge.api.dto.workflow;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Workflow 响应数据模型。
 *
 * @author sxie
 */
@Data
@Builder
public class WorkflowResponse {

    /**
     * 主键ID
     */
    private Long id;
    /**
     * Workflow 编码
     */
    private String workflowCode;
    /**
     * Workflow 名称
     */
    private String workflowName;
    /**
     * 描述
     */
    private String description;
    /**
     * 状态
     */
    private String status;
    /**
     * 当前已发布版本 ID
     */
    private Long currentPublishedVersionId;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
