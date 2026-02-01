package com.xbk.knowledge.api.dto.xxl;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * XXL 任务更新请求
 * 用于提交任务更新参数
 *
 * 职责：接口层 DTO，用于承载请求参数并保证传输边界稳定
 * @author xiexu
 */
@Data
public class XxlJobUpdateRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 任务 ID
     */
    @NotNull(message = "任务 ID 不能为空")
    private Long id;

    /**
     * 任务描述
     */
    @NotBlank(message = "任务描述不能为空")
    private String jobDesc;

    /**
     * 创建人
     */
    @NotBlank(message = "创建人不能为空")
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
    @NotBlank(message = "CRON 不能为空")
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
    @NotBlank(message = "执行器 Handler 不能为空")
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
}
