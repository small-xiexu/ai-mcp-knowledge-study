package com.xbk.knowledge.domain.model.entity;

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
 * 模型能力实体
 * 对应数据库表：ai_model_capability
 *
 * 职责：领域实体，用于承载核心业务状态与生命周期
 * @author xiexu
 */
@TableName("ai_model_capability")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelCapability {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 模型ID（外键）
     */
    private Long modelId;

    /**
     * 最大输入token
     */
    private Integer maxInputTokens;

    /**
     * 最大输出token
     */
    private Integer maxOutputTokens;

    /**
     * 支持函数调用
     */
    private Boolean supportFunctionCalling;

    /**
     * 支持视觉
     */
    private Boolean supportVision;

    /**
     * 支持流式输出
     */
    private Boolean supportStreaming;

    /**
     * 质量评分（1-100）
     */
    private Integer qualityScore;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 关联的模型配置（多对一关系）
     */
    @TableField(exist = false)
    private ModelConfig modelConfig;
}
