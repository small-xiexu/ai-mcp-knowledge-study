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
 * Agent 持久化对象。
 *
 * @author sxie
 */
@TableName("agent")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentPO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String agentCode;

    private String agentName;

    private String description;

    private String status;

    private Long currentPublishedVersionId;

    private Long createdBy;

    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
