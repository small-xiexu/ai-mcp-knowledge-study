package com.xbk.knowledge.api.dto.xxl;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * XXL 任务手动触发请求
 * 用于手动执行任务
 *
 * 职责：接口层 DTO，用于承载请求参数并保证传输边界稳定
 * @author sxie
 */
@Data
public class XxlJobTriggerRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 任务 ID
     */
    @NotNull(message = "任务 ID 不能为空")
    private Long id;

    /**
     * 执行参数
     */
    private String executorParam;

    /**
     * 指定机器地址列表
     */
    private String addressList;
}
