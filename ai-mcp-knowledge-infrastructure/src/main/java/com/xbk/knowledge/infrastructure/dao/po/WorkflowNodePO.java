package com.xbk.knowledge.infrastructure.dao.po;

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
 * 对应表workflow_node
 *
 * @author sxie
 */
@TableName("workflow_node")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowNodePO {

    /**
     * 主键 ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
