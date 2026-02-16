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
 * WorkflowEdge 实体。
 *
 * 对应表：workflow_edge
 
  * @author xiexu
  */
@TableName("workflow_edge")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEdge {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private Long workflowVersionId;

    private String sourceKey;

    private String targetKey;

    /**
     * DEFAULT/TRUE/FALSE/CONDITION
     */
    private String edgeType;

    private String conditionExpr;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

