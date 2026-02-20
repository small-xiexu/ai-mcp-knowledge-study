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
    private Long id;

    private Long workflowVersionId;

    private String nodeKey;

    /**
     * START/LLM/RAG_RETRIEVE/TOOL_CALL/IF/PARALLEL/JOIN/OUTPUT/END
     */
    private String nodeType;

    private String nodeName;

    /**
     * 节点配置 JSON。
     */
    private String configJson;

    private Integer positionX;

    private Integer positionY;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

