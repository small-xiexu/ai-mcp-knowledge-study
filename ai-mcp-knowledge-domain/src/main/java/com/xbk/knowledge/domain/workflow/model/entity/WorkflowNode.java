package com.xbk.knowledge.domain.workflow.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * WorkflowNode 实体。
 *
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowNode {

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * Workflow 版本 ID。
     */
    private Long workflowVersionId;

    /**
     * 节点键。
     */
    private String nodeKey;

    /**
     * START/LLM/RAG_RETRIEVE/TOOL_CALL/IF/PARALLEL/JOIN/OUTPUT/END
     */
    private String nodeType;

    /**
     * 节点名称。
     */
    private String nodeName;

    /**
     * 节点配置 JSON。
     */
    private String configJson;

    /**
     * 画布 X 坐标。
     */
    private Integer positionX;

    /**
     * 画布 Y 坐标。
     */
    private Integer positionY;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
