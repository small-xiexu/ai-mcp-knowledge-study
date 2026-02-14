package com.xbk.knowledge.domain.model.entity.agent;

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
 * Agent 运行上下文快照实体。
 * 对应数据库表：agent_run_context
 *
 * 职责：在审批“方式B”续跑场景中保存可恢复的运行输入快照。
 *
 * @author xiexu
 */
@TableName("agent_run_context")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRunContext {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private String runId;

    /**
     * SAVED/RESUMED/EXPIRED。
     */
    private String status;

    private String snapshotJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

