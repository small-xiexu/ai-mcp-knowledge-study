package com.xbk.knowledge.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * XXL 任务信息实体
 * 承载调度任务的核心字段
 *
 * 职责：领域实体，用于表达任务信息语义
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XxlJobInfo {

    /**
     * 任务 ID
     *
     * 为什么：用于唯一标识任务
     */
    private Long id;

    /**
     * 任务描述
     *
     * 为什么：便于展示与检索
     */
    private String jobDesc;

    /**
     * 执行器 ID
     *
     * 为什么：标识任务所属执行器
     */
    private Integer jobGroup;

    /**
     * 创建人
     *
     * 为什么：用于审计与沟通
     */
    private String author;

    /**
     * 报警邮箱
     *
     * 为什么：任务异常通知
     */
    private String alarmEmail;

    /**
     * 调度类型
     *
     * 为什么：区分 CRON 等不同调度类型
     */
    private String scheduleType;

    /**
     * 执行器 Handler
     *
     * 为什么：定位执行器内的具体任务
     */
    private String executorHandler;

    /**
     * 执行参数
     *
     * 为什么：支持动态传参
     */
    private String executorParam;

    /**
     * CRON 表达式
     *
     * 为什么：定义调度周期
     */
    private String scheduleConf;

    /**
     * 过期策略
     *
     * 为什么：控制错过调度后的处理策略
     */
    private String misfireStrategy;

    /**
     * 路由策略
     *
     * 为什么：控制执行器节点路由方式
     */
    private String executorRouteStrategy;

    /**
     * 阻塞策略
     *
     * 为什么：控制并发执行行为
     */
    private String executorBlockStrategy;

    /**
     * 超时时间
     *
     * 为什么：控制任务执行时限
     */
    private Integer executorTimeout;

    /**
     * 失败重试次数
     *
     * 为什么：控制失败重试上限
     */
    private Integer executorFailRetryCount;

    /**
     * Glue 类型
     *
     * 为什么：标识脚本/Bean 等执行模式
     */
    private String glueType;

    /**
     * 子任务 ID
     *
     * 为什么：支持任务链路
     */
    private String childJobId;

    /**
     * 触发状态
     *
     * 为什么：标识任务启停状态
     */
    private Integer triggerStatus;

    /**
     * 创建时间
     *
     * 为什么：用于审计与排序
     */
    private String addTime;

    /**
     * 更新时间
     *
     * 为什么：用于变更追踪
     */
    private String updateTime;

    /**
     * 上次触发时间
     *
     * 为什么：用于展示与监控
     */
    private Long triggerLastTime;

    /**
     * 下次触发时间
     *
     * 为什么：用于展示与监控
     */
    private Long triggerNextTime;
}
