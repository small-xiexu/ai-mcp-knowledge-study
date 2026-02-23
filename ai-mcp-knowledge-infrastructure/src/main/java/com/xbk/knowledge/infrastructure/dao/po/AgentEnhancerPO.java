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
 * AgentEnhancer 资产实体。
 *
 * 对应表：agent_enhancer
 *
 * @author sxie
 */
@TableName("agent_enhancer")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentEnhancerPO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 对外唯一编码。
     */
    private String agentEnhancerCode;

    private String agentEnhancerName;

    /**
     * AgentEnhancer 类型（用于运行时装配）。
     *
     * 建议值：
     * - CHAT_MEMORY
     * - REQUEST_RESPONSE_LOG
     * - TOOL_CALL_LOG
     */
    private String agentEnhancerType;

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
