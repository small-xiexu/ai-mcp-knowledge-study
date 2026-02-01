package com.xbk.knowledge.api.dto.xxl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * XXL 任务日志响应
 * 用于返回任务日志列表信息
 *
 * 职责：接口层 DTO，用于承载响应参数并保证传输边界稳定
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XxlJobLogResponse {

    /**
     * 日志 ID
     */
    private Long id;

    /**
     * 任务 ID
     */
    private Long jobId;

    /**
     * 执行器地址
     */
    private String executorAddress;

    /**
     * 执行器 Handler
     */
    private String executorHandler;

    /**
     * 执行参数
     */
    private String executorParam;

    /**
     * 分片参数
     */
    private String executorShardingParam;

    /**
     * 失败重试次数
     */
    private Integer executorFailRetryCount;

    /**
     * 触发时间
     */
    private String triggerTime;

    /**
     * 触发结果码
     */
    private Integer triggerCode;

    /**
     * 触发日志
     */
    private String triggerMsg;

    /**
     * 处理时间
     */
    private String handleTime;

    /**
     * 处理结果码
     */
    private Integer handleCode;

    /**
     * 处理日志
     */
    private String handleMsg;

    /**
     * 告警状态
     */
    private Integer alarmStatus;
}
