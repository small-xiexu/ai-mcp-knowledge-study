package com.xbk.knowledge.domain.model.entity.tool;

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
 * 工具风险策略实体。
 * 对应数据库表：tool_policy
 *
 * 职责：承载 toolKey 在某 org 下的风险等级与审批门禁配置。
 *
 * @author xiexu
 */
@TableName("tool_policy")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolPolicy {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private String toolKey;

    private String riskLevel;

    private Integer approvalRequired;

    private Integer enabled;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

