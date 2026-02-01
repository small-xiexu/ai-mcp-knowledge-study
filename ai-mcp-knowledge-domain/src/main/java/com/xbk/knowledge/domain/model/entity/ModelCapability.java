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
     *
     * 为什么：用于持久化唯一标识
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 模型ID（外键）
     *
     * 为什么：关联模型配置
     */
    private Long modelId;

    /**
     * 最大输入token
     *
     * 为什么：限制输入上下文规模
     */
    private Integer maxInputTokens;

    /**
     * 最大输出token
     *
     * 为什么：限制输出内容规模
     */
    private Integer maxOutputTokens;

    /**
     * 支持函数调用
     *
     * 为什么：标识模型工具能力
     */
    private Boolean supportFunctionCalling;

    /**
     * 支持视觉
     *
     * 为什么：标识多模态能力
     */
    private Boolean supportVision;

    /**
     * 支持流式输出
     *
     * 为什么：标识是否支持流式响应
     */
    private Boolean supportStreaming;

    /**
     * 质量评分（1-100）
     *
     * 为什么：用于排序与推荐
     */
    private Integer qualityScore;

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

    /**
     * 关联的模型配置（多对一关系）
     *
     * 为什么：便于聚合操作与 DTO 映射
     */
    @TableField(exist = false)
    private ModelConfig modelConfig;
}
