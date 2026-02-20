package com.xbk.knowledge.api.dto.xxl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * XXL 任务详情响应
 * 用于返回任务详情信息
 *
 * 职责：接口层 DTO，用于承载响应参数并保证传输边界稳定
 * @author sxie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XxlJobDetailResponse {

    /**
     * 任务 ID
     */
    private Long id;

    /**
     * 执行器 ID
     */
    private Integer jobGroup;

    /**
     * 任务描述
     */
    private String jobDesc;

    /**
     * 创建人
     */
    private String author;

    /**
     * 报警邮箱
     */
    private String alarmEmail;

    /**
     * 调度类型
     */
    private String scheduleType;

    /**
     * 调度配置（CRON）
     */
    private String scheduleConf;

    /**
     * 过期策略
     */
    private String misfireStrategy;

    /**
     * 路由策略
     */
    private String executorRouteStrategy;

    /**
     * 执行器 Handler
     */
    private String executorHandler;

    /**
     * 执行参数
     */
    private String executorParam;

    /**
     * 阻塞策略
     */
    private String executorBlockStrategy;

    /**
     * 超时时间
     */
    private Integer executorTimeout;

    /**
     * 失败重试次数
     */
    private Integer executorFailRetryCount;

    /**
     * Glue 类型
     */
    private String glueType;

    /**
     * 子任务 ID
     */
    private String childJobId;

    /**
     * 触发状态
     */
    private Integer triggerStatus;

    /**
     * 上次触发时间
     */
    private Long triggerLastTime;

    /**
     * 下次触发时间
     */
    private Long triggerNextTime;
}
