package com.xbk.knowledge.domain.job.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * XXL 任务日志实体
 * 承载任务日志的核心字段
 *
 * 职责：领域实体，用于表达日志信息语义
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XxlJobLogInfo {

    /**
     * 日志 ID
     *
     * 为什么：用于唯一标识日志
     */
    private Long id;

    /**
     * 执行器 ID
     *
     * 为什么：标识日志所属执行器
     */
    private Integer jobGroup;

    /**
     * 任务 ID
     *
     * 为什么：标识日志所属任务
     */
    private Long jobId;

    /**
     * 执行器地址
     *
     * 为什么：定位具体执行节点
     */
    private String executorAddress;

    /**
     * 执行器 Handler
     *
     * 为什么：标识执行入口
     */
    private String executorHandler;

    /**
     * 执行参数
     *
     * 为什么：记录实际执行参数
     */
    private String executorParam;

    /**
     * 分片参数
     *
     * 为什么：记录分片执行信息
     */
    private String executorShardingParam;

    /**
     * 失败重试次数
     *
     * 为什么：记录重试配置
     */
    private Integer executorFailRetryCount;

    /**
     * 触发时间
     *
     * 为什么：记录触发时间点
     */
    private String triggerTime;

    /**
     * 触发结果码
     *
     * 为什么：标识触发是否成功
     */
    private Integer triggerCode;

    /**
     * 触发日志
     *
     * 为什么：记录调度侧输出
     */
    private String triggerMsg;

    /**
     * 处理时间
     *
     * 为什么：记录执行完成时间
     */
    private String handleTime;

    /**
     * 处理结果码
     *
     * 为什么：标识执行是否成功
     */
    private Integer handleCode;

    /**
     * 处理日志
     *
     * 为什么：记录执行日志摘要
     */
    private String handleMsg;

    /**
     * 告警状态
     *
     * 为什么：标识告警处理结果
     */
    private Integer alarmStatus;
}
