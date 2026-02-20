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
 * 模型激活配置实体
 * 对应数据库表：ai_model_activation
 *
 * 职责：记录当前激活的对话模型与嵌入模型
 * @author sxie
 */
@TableName("ai_model_activation")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelActivationPO {

    /**
     * 主键ID
     *
     * 为什么：用于持久化唯一标识
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * scopeId。
     */

    /**
     * 当前激活的对话模型ID
     *
     * 为什么：全局对话模型唯一激活
     */
    private Long chatModelId;

    /**
     * 当前激活的嵌入模型ID
     *
     * 为什么：全局嵌入模型唯一激活
     */
    private Long embeddingModelId;

    /**
     * 创建时间
     *
     * 为什么：用于审计与排序
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     *
     * 为什么：用于审计与变更追踪
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
