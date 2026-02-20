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
 * WorkflowVersion 实体（草稿/发布/历史）。
 *
 * 对应表：workflow_version
 *
 * @author sxie
 */
@TableName("workflow_version")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowVersionPO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long workflowId;

    private Integer versionNo;

    /**
     * DRAFT/PUBLISHED/ARCHIVED。
     */
    private String state;

    private String changeSummary;

    /**
     * 画布快照（nodes+edges+viewport），用于前端回显与审计回放。
     */
    private String graphJson;

    /**
     * 默认配置（JSON），节点可继承并覆盖。
     */
    private String defaultConfigJson;

    private Long createdBy;

    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

