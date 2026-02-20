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
 * PromptTemplate 实体（模板资产）。
 *
 * 对应表：prompt_template
 *
 * @author sxie
 */
@TableName("prompt_template")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplatePO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String templateCode;

    private String templateName;

    /**
     * 版本号（建议：draft=0，published>=1）。
     */
    private Integer versionNo;

    /**
     * DRAFT/PUBLISHED/ARCHIVED。
     */
    private String state;

    private String content;

    private String variableSpecJson;

    private Long createdBy;

    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
