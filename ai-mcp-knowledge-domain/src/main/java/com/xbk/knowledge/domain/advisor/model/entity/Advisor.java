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
 * Advisor 资产实体。
 *
 * 对应表：advisor
 
  * @author xiexu
  */
@TableName("advisor")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Advisor {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 对外唯一编码（同 scope 内唯一）。
     */
    private String advisorCode;

    private String advisorName;

    /**
     * Advisor 类型（用于运行时装配）。
     *
     * 建议值：
     * - CHAT_MEMORY
     * - REQUEST_RESPONSE_LOG
     * - TOOL_CALL_LOG
     */
    private String advisorType;

    /**
     * 是否启用（1启用 0禁用）。
     */
    private Integer enabled;

    /**
     * 类型配置（JSON 字符串）。
     */
    private String configJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

