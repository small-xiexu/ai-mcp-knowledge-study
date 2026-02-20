package com.xbk.knowledge.domain.advisor.model.entity;

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
 * Advisor 绑定关系实体。
 *
 * 对应表：advisor_binding
 *
 * @author sxie
 */
@TableName("advisor_binding")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvisorBinding {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 绑定类型：AGENT_VERSION/WORKFLOW_VERSION。
     */
    private String bindType;

    /**
     * 绑定目标 ID：AgentVersionId / WorkflowVersionId。
     */
    private Long bindTargetId;

    private Long advisorId;

    /**
     * 排序序号（越小越先执行）。
     */
    private Integer orderNo;

    /**
     * 是否启用（1启用 0禁用）。
     */
    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

