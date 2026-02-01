package com.xbk.knowledge.api.dto.xxl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * XXL 任务响应
 * 用于返回任务列表基础信息
 *
 * 职责：接口层 DTO，用于承载响应参数并保证传输边界稳定
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XxlJobResponse {

    /**
     * 任务 ID
     */
    private Long id;

    /**
     * 任务描述
     */
    private String jobDesc;

    /**
     * 执行器 Handler
     */
    private String executorHandler;

    /**
     * 执行参数
     */
    private String executorParam;

    /**
     * CRON 表达式
     */
    private String scheduleConf;

    /**
     * 路由策略
     */
    private String executorRouteStrategy;

    /**
     * 触发状态
     */
    private Integer triggerStatus;

    /**
     * 创建人
     */
    private String author;

    /**
     * 创建时间
     */
    private String addTime;

    /**
     * 更新时间
     */
    private String updateTime;
}
