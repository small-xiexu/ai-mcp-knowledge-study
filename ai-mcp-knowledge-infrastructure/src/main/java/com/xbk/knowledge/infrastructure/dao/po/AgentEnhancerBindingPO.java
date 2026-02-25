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
 * AgentEnhancer 绑定关系实体。
 *
 * 对应表agent_enhancer_binding
 *
 * @author sxie
 */
@TableName("agent_enhancer_binding")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentEnhancerBindingPO {

    /**
     * 主键 ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 绑定类型AGENT_VERSION/WORKFLOW_VERSION。
     */
    private String bindType;

    /**
     * 绑定目标 IDAgentVersionId / WorkflowVersionId。
     */
    private Long bindTargetId;

    /**
     * Agent 增强器 ID。
     */
    private Long agentEnhancerId;

    /**
     * 排序序号（越小越先执行）。
     */
    private Integer orderNo;

    /**
     * 是否启用（1启用 0禁用）。
     */
    private Integer enabled;

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
