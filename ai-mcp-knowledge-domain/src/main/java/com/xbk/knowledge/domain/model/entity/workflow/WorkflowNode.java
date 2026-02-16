package com.xbk.knowledge.domain.model.entity.workflow;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * WorkflowNode 实体。
 *
 * 对应表：workflow_node
 
  * @author xiexu
  */
@TableName("workflow_node")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowNode {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long orgId;

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

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

