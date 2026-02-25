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
 * WorkflowEdge 实体。
 *
 * 对应表workflow_edge
 *
 * @author sxie
 */
@TableName("workflow_edge")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEdgePO {

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
     * 源节点键。
     */
    private String sourceKey;

    /**
     * 目标节点键。
     */
    private String targetKey;

    /**
     * DEFAULT/TRUE/FALSE/CONDITION
     */
    private String edgeType;

    /**
     * 条件表达式。
     */
    private String conditionExpr;

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
