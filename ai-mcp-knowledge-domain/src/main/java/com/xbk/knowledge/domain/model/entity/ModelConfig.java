package com.xbk.knowledge.domain.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xbk.knowledge.types.enums.ModelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 模型配置实体
 * 对应数据库表：ai_model_config
 *
 * 职责：领域实体，用于承载核心业务状态与生命周期
 * @author xiexu
 */
@TableName("ai_model_config")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfig {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 模型类型（OPENAI/ANTHROPIC/GEMINI）
     */
    private ModelType modelType;

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * API地址
     */
    private String baseUrl;

    /**
     * 是否启用（0:禁用 1:启用）
     */
    private Boolean enabled;

    /**
     * 优先级（数字越大优先级越高）
     */
    private Integer priority;

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
     * 模型能力（一对一关系）
     */
    @TableField(exist = false)
    private ModelCapability capability;
}
