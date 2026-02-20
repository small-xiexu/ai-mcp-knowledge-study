package com.xbk.knowledge.domain.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xbk.knowledge.types.enums.RagTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * RAG 任务实体
 * 对应数据库表：ai_rag_task
 *
 * 职责：记录知识库构建任务
 * @author xiexu
 */
@TableName("ai_rag_task")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagTask {

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
     * 任务ID（业务标识）
     *
     * 为什么：用于业务侧追踪任务
     */
    private String taskId;

    /**
     * 任务类型（FILE/GIT）
     *
     * 为什么：区分任务来源与处理方式
     */
    private String type;

    /**
     * 任务状态
     *
     * 为什么：用于进度展示与告警
     */
    private RagTaskStatus status;

    /**
     * 任务进度（0-100）
     *
     * 为什么：用于进度展示
     */
    private Integer progress;

    /**
     * 状态描述
     *
     * 为什么：用于展示当前状态说明
     */
    private String message;

    /**
     * 知识库标签
     *
     * 为什么：标识任务归属的知识库
     */
    private String ragTag;

    /**
     * 失败详情（JSON 格式）
     * 存储失败文件的详细信息
     *
     * 为什么：用于重试与排查
     */
    private String errorDetails;

    /**
     * 任务级重试次数
     * 仅用于任务级重试（手动重试或定时重试），不记录文件级重试次数
     *
     * 为什么：记录任务级重试链路
     */
    private Integer retryCount;

    /**
     * 父任务 ID
     * 用于建立任务谱系，重试任务关联原任务
     *
     * 为什么：追踪重试来源
     */
    private String parentTaskId;

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
